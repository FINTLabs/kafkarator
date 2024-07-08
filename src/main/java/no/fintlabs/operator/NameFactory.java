package no.fintlabs.operator;

import io.fabric8.kubernetes.api.model.HasMetadata;

public class NameFactory {

    public static final int MAX_LENGTH = 64;

    public static String generateNameFromMetadata(HasMetadata metadata) {

        // flais-io_flais_fint-data-service


        String name = nameFromMetadata(metadata);
        String orgId = metadata.getMetadata().getLabels().get("fintlabs.no/org-id").replace(".", "-");
        String team = metadata.getMetadata().getLabels().get("fintlabs.no/team");

        // Remove org-id if name is too long
        if (nameFromMetadataLength(metadata) >= MAX_LENGTH && name.contains(orgId)) {
            name = name.replace(orgId + "__", "");
        }

        // Remove -io from org-id if name is too long
        if (nameFromMetadataLength(metadata) >= MAX_LENGTH && name.contains("-io_")) {
            name = name.replace("-io_", "-");
        }

        // Remove team if name is too long
        if (nameFromMetadataLength(metadata) >= MAX_LENGTH && name.contains("_" + team)) {
            name = name.replace("_" + team + "_", "");
        }

        return name;

    }

    public static int nameFromMetadataLength(HasMetadata metadata) {
        return nameFromMetadata(metadata).length();
    }

    public static String nameFromMetadata(HasMetadata metadata) {
        return String.format("%s_%s_%s",
                metadata.getMetadata().getLabels().get("fintlabs.no/org-id").replace(".", "-"),
                metadata.getMetadata().getLabels().get("fintlabs.no/team"),
                metadata.getMetadata().getName()
        );
    }
}
