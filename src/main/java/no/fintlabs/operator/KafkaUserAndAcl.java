package no.fintlabs.operator;

import lombok.*;
import no.fintlabs.aiven.CreateKafkaAclEntryResponse;
import no.fintlabs.aiven.CreateKafkaUserResponse;
import no.fintlabs.aiven.KafkaAclEntry;
import no.fintlabs.aiven.AivenServiceUser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class KafkaUserAndAcl {
    private AivenServiceUser user;
    private List<KafkaAclEntry> aclEntries = new ArrayList<>();

    public static KafkaUserAndAcl fromUserAndAclResponse(CreateKafkaUserResponse user, CreateKafkaAclEntryResponse acl) {
        return KafkaUserAndAcl.builder()
                .user(user.getUser())
                .aclEntries(
                        acl
                                .getKafkaAclEntry()
                                .stream()
                                .filter(kafkaAclEntry -> kafkaAclEntry.getUsername().equals(user.getUser().getUsername()))
                                .collect(Collectors.toList())
                )
                .build();
    }
}
