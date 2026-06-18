package no.fintlabs.operator;

import io.fabric8.kubernetes.api.model.HasMetadata;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import static no.fintlabs.operator.Constants.*;

public class NameFactory {
    public static String userName(HasMetadata metadata, boolean useV2) {
        if (useV2) {
            return serviceUserName(
                    metadata.getMetadata().getLabels().get(ORG_ID_LABEL),
                    metadata.getMetadata().getLabels().get(TEAM_LABEL),
                    metadata.getMetadata().getName()
            );
        }

        return legacyUserName(metadata);
    }

    public static String serviceUserName(
            String orgName,
            String teamName,
            String appName
    ) {
        String hash = hashedName(teamName, appName, orgName);

        return "%s_%s_%s_%s".formatted(
                shortOrgName(orgName),
                shortTeamName(teamName),
                shortAppName(teamName, appName),
                hash
        );
    }

    public static String legacyUserName(HasMetadata metadata) {
        return String.format("%s_%s_%s",
                metadata.getMetadata().getLabels().get(ORG_ID_LABEL).replace(".", "-"),
                metadata.getMetadata().getLabels().get(TEAM_LABEL),
                metadata.getMetadata().getName()
        );
    }

    public static boolean usesNameVersionV2(HasMetadata metadata) {
        var annotations = metadata.getMetadata().getAnnotations();

        return annotations != null
                && NAME_VERSION_V2.equals(annotations.get(NAME_VERSION_ANNOTATION));
    }

    private static String hashedName(String teamName, String appName, String orgName) {
        CRC32 crc32 = new CRC32();
        String baseName = teamName + appName + orgName;
        byte[] bytes = baseName.getBytes(StandardCharsets.UTF_8);

        crc32.update(bytes, 0, bytes.length);

        return "%08x".formatted(crc32.getValue());
    }

    private static String shortOrgName(String orgName) {
        return shorten(orgName, null, "_no", ORG_NAME_LENGTH);
    }

    private static String shortTeamName(String teamName) {
        return shorten(teamName, "team", null, TEAM_NAME_LENGTH);
    }

    private static String shortAppName(String teamName, String appName) {
        return shorten(appName, teamName, null, APP_NAME_LENGTH);
    }

    private static String shorten(String input, String prefix, String suffix, int maxLen) {
        input = input
                .toLowerCase()
                .replace(".", "_")
                .replaceAll("[^a-z0-9_-]", "");

        if (prefix != null && input.startsWith(prefix) && !input.equals(prefix)) {
            input = input.substring(prefix.length());
        }

        if (suffix != null && input.endsWith(suffix) && !input.equals(suffix)) {
            input = input.substring(0, input.length() - suffix.length());
        }

        while (!input.isEmpty() && input.charAt(0) == '-') {
            input = input.substring(1);
        }

        if (input.length() > maxLen) {
            input = input.substring(0, maxLen);
        }

        while (input.length() > 1 && input.charAt(input.length() - 1) == '-') {
            input = input.substring(0, input.length() - 1);
        }

        return input;
    }
}
