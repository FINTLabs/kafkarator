package no.fintlabs.operator;

import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.UUID;

public class NameFactory {

    public static String legacyNameFromMetadata(HasMetadata metadata) {
        return String.format("%s_%s_%s",
                metadata.getMetadata().getLabels().get("fintlabs.no/org-id").replace(".", "-"),
                metadata.getMetadata().getLabels().get("fintlabs.no/team"),
                metadata.getMetadata().getName()
        );
    }

    public static String guidNameFromMetadata() {
        return UUID.randomUUID().toString();
    }

    public static String nameFromMetadata(HasMetadata metadata) {
        String useGuid = metadata.getMetadata().getLabels().get("fintlabs.no/use-guid");
        if ("true".equalsIgnoreCase(useGuid)) {
            return guidNameFromMetadata();
        } else {
            return legacyNameFromMetadata(metadata);
        }
    }
}
