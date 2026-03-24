package no.fintlabs.operator;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import no.fintlabs.keystore.KeyStoreService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CertificateMetricsService {

    private final MeterRegistry meterRegistry;
    private final Map<ResourceKey, GaugeValues> resourceGauges = new ConcurrentHashMap<>();

    public CertificateMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge.builder("kafkarator_certificate_oldest_days_until_expiry", resourceGauges, this::oldestDaysUntilExpiry)
                .description("Lowest number of days remaining before any managed Kafka client certificate expires")
                .register(meterRegistry);
    }

    public Timer.Sample startReconcile() {
        return Timer.start(meterRegistry);
    }

    public void recordReconcile(KafkaUserAndAclCrd primary, String serviceName, Timer.Sample sample) {
        sample.stop(Timer.builder("kafkarator_certificate_secret_reconcile_duration_seconds")
                .description("Duration of certificate secret reconcile operations")
                .tags(aggregateTags(primary, serviceName))
                .register(meterRegistry));
    }

    public void recordInspection(KafkaUserAndAclCrd primary,
                                 String serviceName,
                                 KeyStoreService.KeyStoreInspection inspection,
                                 Duration threshold) {
        Counter.builder("kafkarator_certificate_inspections_total")
                .description("Number of certificate inspections performed during reconcile")
                .tags(withResultTags(primary, serviceName, inspectionResult(inspection, threshold)))
                .register(meterRegistry)
                .increment();
    }

    public void updateResourceState(KafkaUserAndAclCrd primary,
                                    String serviceName,
                                    KeyStoreService.KeyStoreInspection inspection,
                                    Duration threshold) {
        GaugeValues values = resourceGauges.computeIfAbsent(
                new ResourceKey(
                        primary.getMetadata().getNamespace(),
                        primary.getMetadata().getName(),
                        team(primary),
                        service(serviceName)
                ),
                this::registerResourceGauges
        );

        values.expiryEpochSeconds = inspection.notAfter() == null ? Double.NaN : inspection.notAfter().getEpochSecond();
        values.daysUntilExpiry = inspection.notAfter() == null
                ? Double.NaN
                : Duration.between(Instant.now(), inspection.notAfter()).toSeconds() / 86400.0d;
        values.rotationDue = inspection.needsRotation(threshold) ? 1.0d : 0.0d;
    }

    public void recordRotation(KafkaUserAndAclCrd primary, String serviceName, boolean success, String reason) {
        Counter.builder("kafkarator_certificate_rotations_total")
                .description("Number of Kafka client certificate rotations attempted by Kafkarator")
                .tags(withRotationTags(primary, serviceName, success, reason))
                .register(meterRegistry)
                .increment();
    }

    private GaugeValues registerResourceGauges(ResourceKey key) {
        GaugeValues values = new GaugeValues();
        Gauge.builder("kafkarator_certificate_expiry_seconds", values, GaugeValues::getExpiryEpochSeconds)
                .description("Unix epoch seconds at which the managed Kafka client certificate expires")
                .tags(resourceTags(key))
                .register(meterRegistry);
        Gauge.builder("kafkarator_certificate_days_until_expiry", values, GaugeValues::getDaysUntilExpiry)
                .description("Days remaining before the managed Kafka client certificate expires")
                .tags(resourceTags(key))
                .register(meterRegistry);
        Gauge.builder("kafkarator_certificate_rotation_due", values, GaugeValues::getRotationDue)
                .description("Whether the managed Kafka client certificate is due for rotation within the configured threshold")
                .tags(resourceTags(key))
                .register(meterRegistry);
        return values;
    }

    private Iterable<Tag> resourceTags(ResourceKey key) {
        return List.of(
                Tag.of("namespace", key.namespace()),
                Tag.of("name", key.name()),
                Tag.of("team", key.team()),
                Tag.of("service", key.service())
        );
    }

    private Iterable<Tag> aggregateTags(KafkaUserAndAclCrd primary, String serviceName) {
        return List.of(
                Tag.of("team", team(primary)),
                Tag.of("service", service(serviceName))
        );
    }

    private Iterable<Tag> withResultTags(KafkaUserAndAclCrd primary, String serviceName, String result) {
        return List.of(
                Tag.of("team", team(primary)),
                Tag.of("service", service(serviceName)),
                Tag.of("result", result)
        );
    }

    private Iterable<Tag> withRotationTags(KafkaUserAndAclCrd primary, String serviceName, boolean success, String reason) {
        return List.of(
                Tag.of("team", team(primary)),
                Tag.of("service", service(serviceName)),
                Tag.of("result", success ? "success" : "failure"),
                Tag.of("reason", sanitize(reason))
        );
    }

    private String inspectionResult(KeyStoreService.KeyStoreInspection inspection, Duration threshold) {
        if (!inspection.readable()) {
            return "unreadable";
        }
        if (inspection.notAfter() != null && !inspection.notAfter().isAfter(Instant.now())) {
            return "expired";
        }
        if (inspection.needsRotation(threshold)) {
            return "expiring";
        }
        return "healthy";
    }

    private double oldestDaysUntilExpiry(Map<ResourceKey, GaugeValues> gauges) {
        return gauges.values().stream()
                .mapToDouble(GaugeValues::getDaysUntilExpiry)
                .filter(value -> !Double.isNaN(value))
                .min()
                .orElse(Double.NaN);
    }

    private static String team(KafkaUserAndAclCrd primary) {
        String value = primary.getMetadata().getLabels().get("fintlabs.no/team");
        return sanitize(value);
    }

    private static String service(String serviceName) {
        return sanitize(serviceName);
    }

    private static String sanitize(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private record ResourceKey(String namespace, String name, String team, String service) {
    }

    private static final class GaugeValues {
        private volatile double expiryEpochSeconds = Double.NaN;
        private volatile double daysUntilExpiry = Double.NaN;
        private volatile double rotationDue = Double.NaN;

        private double getExpiryEpochSeconds() {
            return expiryEpochSeconds;
        }

        private double getDaysUntilExpiry() {
            return daysUntilExpiry;
        }

        private double getRotationDue() {
            return rotationDue;
        }
    }
}
