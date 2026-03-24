# Fint Kafkarator

Fint Kafkarator is a Kubernetes operator that provisions Kafka service users and ACLs in Aiven, and publishes the client configuration and certificate material as Kubernetes secrets.

Runtime stack:

- Spring Boot `3.5.x`
- Java `25`
- Gradle `9.4.x`

## What does the operator do?

When a `KafkaUserAndAcl` resource is created:
- The operator creates a service user and ACLs in Aiven.
- The operator creates a `-kafka` secret with Spring Kafka SSL configuration.
- The operator creates a `-kafka-certificates` secret with `client.keystore.p12` and `client.truststore.jks`.

When a `KafkaUserAndAcl` resource is deleted:
- The operator deletes the user and ACLs from Aiven.
- The operator deletes the managed secrets from Kubernetes.

When an existing certificate secret is reconciled:
- The operator inspects the current client certificate expiry date.
- The operator rotates the keystore and truststore if the certificate is missing, unreadable, or inside the configured rotation threshold.
- The operator annotates the secret with the observed certificate expiry and last rotation time.

## Operational Improvements

Operationally relevant improvements:

- Expiry-aware certificate handling instead of only verifying that the keystore can be opened.
- Configurable certificate rotation threshold via `fint.aiven.certificate-rotation-threshold`.
- Prometheus metrics for certificate expiry, rotation pressure, inspections, rotations and reconcile duration.
- Grafana/PromQL documentation for dashboards and alerting.

See:

- [PromQL examples](docs/metrics-promql.md)
- [Grafana dashboard JSON](docs/kafkarator-grafana-dashboard.json)

## Custom Resource

### KafkaUserAndAcl
```yaml
apiVersion: "fintlabs.no/v1alpha1"
kind: KafkaUserAndAcl
metadata:
  name: <name>
  labels:
    app.kubernetes.io/name: <name>
    app.kubernetes.io/instance: <instance>
    app.kubernetes.io/version: <version>
    app.kubernetes.io/component: <component>
    app.kubernetes.io/part-of: <part-of>
    fintlabs.no/team: <team>
spec:
  acls:
    - permission: <read | readwrite | write>
      topic: '<topic>'
```

### Example of Custom Resource

```yaml
apiVersion: "fintlabs.no/v1alpha1"
kind: KafkaUserAndAcl
metadata:
  name: sample-user
  labels:
    app.kubernetes.io/name: sample-user
    app.kubernetes.io/instance: sample-test
    app.kubernetes.io/version: latest
    app.kubernetes.io/component: sample
    app.kubernetes.io/part-of: sample-test
    fintlabs.no/team: sample-test
spec:
  acls:
    - permission: read
      topic: '*sample-test'
    - permission: read
      topic: '*sample-test2'
```

## Prerequisites

- Aiven account, project and service
- Aiven token and Aiven API base URL in `application.yaml`

## Configuration

Relevant application properties:

```yaml
fint:
  aiven:
    base-url: https://api.aiven.io/v1
    project: fintlabs
    service: kafka-alpha
    kafka-bootstrap-servers: broker-1:9092,broker-2:9092
    certificate-rotation-threshold: 30d
```

## Metrics

Kafkarator exposes Prometheus metrics on `/actuator/prometheus`.

Key metrics:

- `kafkarator_certificate_expiry_seconds`
- `kafkarator_certificate_days_until_expiry`
- `kafkarator_certificate_rotation_due`
- `kafkarator_certificate_oldest_days_until_expiry`
- `kafkarator_certificate_inspections_total`
- `kafkarator_certificate_rotations_total`
- `kafkarator_certificate_secret_reconcile_duration_seconds`

## Building And Testing

Run the full test suite:

```bash
./gradlew test
```
