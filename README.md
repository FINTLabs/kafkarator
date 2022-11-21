# Fint Aivenerator

Fint Aivenerator is an operator that creates a service user and ACL in Aiven. 
Username, password, ACL id and access certificate and -key will be stored in kubernetes secrets

## What does the operator do?

When a `AivenKafkaAcl` CR is **created**:
* The operator will create a service user and ACL in Aiven.
* Username, password and ACL id will be generated and stored in secrets along with access certificate and -key.

When a `AivenKafkaAcl` CR is **deleted**:
* The operator will delete the user and ACL from Aiven. 
* The operator will delete the secrets from Kubernetes.

## How to use the operator:

### AivenKafkaAcl
```yaml
apiVersion: "fintlabs.no/v1alpha1"
kind: AivenKafkaAcl
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
  kafkaAclEntry:
    - permission: <read | readwrite | write>
      topic: '<topic>'
```

### Example of Custom Resource

```yaml
apiVersion: "fintlabs.no/v1alpha1"
kind: AivenKafkaAcl
metadata:
  name: sample-user
  labels:
    app.kubernetes.io/name: sample-user
    app.kubernetes.io/instance: sample-test
    app.kubernetes.io/version: latest
    app.kubernetes.io/component: sample
    app.kubernetes.io/'part-of: sample-test
    fintlabs.no/team: sample-test
spec:
  acls:
    - permission: read
      topic: '*sample-test'
    - permission: read
      topic: '*sample-test2'
```

#### Prerequisites
* Aiven account, project and service
* Aiven token and Aiven api base url in application.yaml

### Using the operator
TODO