# Certificate Rotation Behavior

This document describes how Kafkarator handles existing certificate secrets, expiry-aware rotation, and metadata updates.

## Reconcile Decision Flow

```mermaid
flowchart TD
    A["KafkaUserAndAcl reconcile starts"] --> B["Load KafkaUserAndAcl secondary resource"]
    B --> C["Load <name>-kafka secret"]
    C --> D["Read key store password and trust store password"]
    D --> E["Load existing <name>-kafka-certificates secret if present"]
    E --> F{"Existing client.keystore.p12 present?"}

    F -- "No" --> G["Mark rotation required"]
    F -- "Yes" --> H["Inspect keystore and extract leaf certificate notAfter"]

    H --> I{"Keystore readable?"}
    I -- "No" --> G
    I -- "Yes" --> J{"Certificate expires within rotation threshold?"}
    J -- "Yes" --> G
    J -- "No" --> K["Reuse existing keystore"]

    G --> L["Generate new keystore from Aiven access_cert/access_key and CA"]
    L --> M["Generate new truststore from Aiven CA"]

    K --> N{"Existing truststore reusable?"}
    N -- "Yes" --> O["Reuse existing truststore"]
    N -- "No" --> P["Generate new truststore from Aiven CA"]

    M --> Q["Inspect resulting keystore"]
    O --> Q
    P --> Q

    Q --> R["Update annotations"]
    R --> R1["Set certificate-not-after"]
    R1 --> R2{"Did rotation happen?"}
    R2 -- "Yes" --> R3["Set last-rotated-at"]
    R2 -- "No" --> S["Keep existing last-rotated-at as-is"]
    R3 --> T["Write <name>-kafka-certificates secret"]
    S --> T

    T --> U["Publish metrics for inspection, rotation, and reconcile duration"]
    U --> V["Reconcile completes"]
```

## Existing Secret Adoption After Deploy

This diagram shows what happens after deploying the new Kafkarator version into a cluster with existing secrets that do not yet have the new annotations.

```mermaid
sequenceDiagram
    participant O as "Kafkarator"
    participant CR as "KafkaUserAndAcl"
    participant KS as "<name>-kafka Secret"
    participant CS as "<name>-kafka-certificates Secret"
    participant A as "Aiven"
    participant M as "Prometheus Metrics"

    O->>CR: Reconcile custom resource
    O->>KS: Read Kafka SSL passwords
    O->>CS: Read existing keystore/truststore and annotations

    alt "No certificate annotations on existing secret"
        O->>CS: Inspect client.keystore.p12
        alt "Keystore readable and cert outside threshold"
            O->>CS: Patch secret metadata with certificate-not-after
            Note over O,CS: last-rotated-at remains absent until an actual rotation happens
        else "Keystore unreadable, expired, missing, or inside threshold"
            O->>A: Use current Aiven credentials and CA
            O->>CS: Regenerate keystore and truststore
            O->>CS: Write certificate-not-after and last-rotated-at
        end
    else "Annotations already present"
        O->>CS: Re-evaluate actual keystore state
        Note over O,CS: annotations are informative, not the source of truth
    end

    O->>M: Publish inspection counters and resource gauges
    O->>M: Publish rotation counters if rotation was attempted
    O->>M: Publish reconcile duration
```

## Backward Compatibility Summary

```mermaid
flowchart LR
    A["Existing secret without annotations"] --> B["Secret is still accepted"]
    B --> C["Kafkarator inspects actual keystore content"]
    C --> D{"Healthy certificate?"}
    D -- "Yes" --> E["Backfill certificate-not-after annotation only"]
    D -- "No" --> F["Rotate keystore/truststore and write both annotations"]
```

## Notes

- The annotations are derived metadata, not required input.
- Missing annotations do not break reconcile.
- The actual keystore content remains the source of truth.
- A large number of old secrets may be patched shortly after rollout, either to backfill annotations or to rotate certificates that are already due.
