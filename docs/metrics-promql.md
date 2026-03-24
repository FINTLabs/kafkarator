# Kafkarator Metrics And PromQL

Kafkarator exposes Prometheus metrics on `/actuator/prometheus`.

The examples below assume the time series use the label `job="kafkarator"`. If your scrape configuration uses different labels, adjust the filters accordingly.

## Available Metrics

- `kafkarator_certificate_expiry_seconds`
- `kafkarator_certificate_days_until_expiry`
- `kafkarator_certificate_rotation_due`
- `kafkarator_certificate_oldest_days_until_expiry`
- `kafkarator_certificate_inspections_total`
- `kafkarator_certificate_rotations_total`
- `kafkarator_certificate_secret_reconcile_duration_seconds_count`
- `kafkarator_certificate_secret_reconcile_duration_seconds_sum`
- `kafkarator_certificate_secret_reconcile_duration_seconds_max`

Resource gauges are tagged with:

- `namespace`
- `name`
- `team`
- `service`

Counters and the reconcile timer are tagged with:

- `team`
- `service`

Additional labels:

- `result` on `kafkarator_certificate_inspections_total`
- `result` and `reason` on `kafkarator_certificate_rotations_total`

## Grafana Panels

### Lowest days until expiry

```promql
min(kafkarator_certificate_days_until_expiry{job="kafkarator"})
```

Use as a stat panel.

### Number of certificates due for rotation now

```promql
count(kafkarator_certificate_rotation_due{job="kafkarator"} > 0)
```

Use as a stat panel.

### Number of certificates expiring within 30 days

```promql
count(kafkarator_certificate_days_until_expiry{job="kafkarator"} < 30)
```

Use as a stat panel.

### 20 certificates closest to expiry

```promql
bottomk(20, kafkarator_certificate_days_until_expiry{job="kafkarator"})
```

Use as a table panel. Show the labels `namespace`, `name`, `team`, `service`.

### All certificates sorted by days until expiry

```promql
sort(kafkarator_certificate_days_until_expiry{job="kafkarator"})
```

Use as a table panel.

### Certificates currently due for rotation

```promql
kafkarator_certificate_rotation_due{job="kafkarator"} > 0
```

Use as a table panel. Show the labels `namespace`, `name`, `team`, `service`.

### Certificates due for rotation by team

```promql
sum by (team) (kafkarator_certificate_rotation_due{job="kafkarator"} > 0)
```

Use as a bar chart.

### Certificates due for rotation by namespace

```promql
sum by (namespace) (kafkarator_certificate_rotation_due{job="kafkarator"} > 0)
```

Use as a bar chart.

### Rotations by result and reason

```promql
sum by (result, reason) (
  rate(kafkarator_certificate_rotations_total{job="kafkarator"}[5m])
)
```

Use as a time series panel.

### Rotations by team

```promql
sum by (team, result) (
  rate(kafkarator_certificate_rotations_total{job="kafkarator"}[5m])
)
```

Use as a stacked time series or bar chart.

### Inspections by result

```promql
sum by (result) (
  rate(kafkarator_certificate_inspections_total{job="kafkarator"}[5m])
)
```

Use as a time series panel.

### Average reconcile duration

```promql
sum(rate(kafkarator_certificate_secret_reconcile_duration_seconds_sum{job="kafkarator"}[5m]))
/
sum(rate(kafkarator_certificate_secret_reconcile_duration_seconds_count{job="kafkarator"}[5m]))
```

Use as a stat panel or time series panel.

### Average reconcile duration by service

```promql
sum by (service) (
  rate(kafkarator_certificate_secret_reconcile_duration_seconds_sum{job="kafkarator"}[5m])
)
/
sum by (service) (
  rate(kafkarator_certificate_secret_reconcile_duration_seconds_count{job="kafkarator"}[5m])
)
```

Use as a time series panel.

### Max reconcile duration over the last 15 minutes

```promql
max_over_time(kafkarator_certificate_secret_reconcile_duration_seconds_max{job="kafkarator"}[15m])
```

Use as a stat panel or time series panel.

## Alert Queries

### Critical: at least one certificate has expired

```promql
min(kafkarator_certificate_days_until_expiry{job="kafkarator"}) < 0
```

Recommended `for`: `15m`

### High: certificate expires within 7 days

```promql
kafkarator_certificate_days_until_expiry{job="kafkarator"} < 7
```

Recommended `for`: `30m`

### Warning: certificate expires within 30 days

```promql
kafkarator_certificate_days_until_expiry{job="kafkarator"} < 30
```

Recommended `for`: `2h`

### High: certificate remains due for rotation over time

```promql
kafkarator_certificate_rotation_due{job="kafkarator"} > 0
```

Recommended `for`: `6h`

### Critical: rotation fails

```promql
increase(kafkarator_certificate_rotations_total{job="kafkarator",result="failure"}[15m]) > 0
```

Recommended `for`: `0m`

### Warning: no inspections in the last hour

```promql
sum(increase(kafkarator_certificate_inspections_total{job="kafkarator"}[1h])) == 0
```

Recommended `for`: `1h`

## Useful Drilldown Queries

### Show a single namespace

```promql
sort(kafkarator_certificate_days_until_expiry{job="kafkarator",namespace="$namespace"})
```

### Show a single team

```promql
sort(kafkarator_certificate_days_until_expiry{job="kafkarator",team="$team"})
```

### Find resources with negative time until expiry

```promql
kafkarator_certificate_days_until_expiry{job="kafkarator"} < 0
```

### Find resources reported as unreadable

```promql
sum by (team, service) (
  increase(kafkarator_certificate_inspections_total{job="kafkarator",result="unreadable"}[1h])
)
```

## Recommended Dashboard Layout

Top row:

- `min(kafkarator_certificate_days_until_expiry{job="kafkarator"})`
- `count(kafkarator_certificate_rotation_due{job="kafkarator"} > 0)`
- `count(kafkarator_certificate_days_until_expiry{job="kafkarator"} < 30)`
- `increase(kafkarator_certificate_rotations_total{job="kafkarator",result="failure"}[24h])`

Middle row:

- `bottomk(20, kafkarator_certificate_days_until_expiry{job="kafkarator"})`
- `sum by (team) (kafkarator_certificate_rotation_due{job="kafkarator"} > 0)`
- `sum by (result, reason) (rate(kafkarator_certificate_rotations_total{job="kafkarator"}[5m]))`

Bottom row:

- `sum by (result) (rate(kafkarator_certificate_inspections_total{job="kafkarator"}[5m]))`
- average reconcile duration
- max reconcile duration over the last 15 minutes
