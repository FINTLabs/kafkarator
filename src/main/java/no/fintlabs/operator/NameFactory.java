package no.fintlabs.operator;

import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.UUID;

public class NameFactory {

    public static String nameFromMetadata(HasMetadata metadata) {

        String orgId = metadata.getMetadata().getLabels().get("fintlabs.no/org-id").replace(".", "-");
        String team = metadata.getMetadata().getLabels().get("fintlabs.no/team");
        String name = metadata.getMetadata().getName();
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        int maxCombinedPrefix = 64 - 19;
        int totalPrefixLength = orgId.length() + team.length() + name.length();
        if (totalPrefixLength > maxCombinedPrefix) {
            double orgRatio = (double) orgId.length() / totalPrefixLength;
            double teamRatio = (double) team.length() / totalPrefixLength;
            double nameRatio = (double) name.length() / totalPrefixLength;

            int maxOrgLength = (int) Math.floor(maxCombinedPrefix * orgRatio);
            int maxTeamLength = (int) Math.floor(maxCombinedPrefix * teamRatio);
            int maxNameLength = maxCombinedPrefix - maxOrgLength - maxTeamLength;

            orgId = orgId.substring(0, Math.min(orgId.length(), maxOrgLength));
            team = team.substring(0, Math.min(team.length(), maxTeamLength));
            name = name.substring(0, Math.min(name.length(), maxNameLength));
        }

        return String.format("%s-%s-%s-%s", orgId, team, name, uuid);
    }
}
