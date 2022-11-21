package no.fintlabs.operator;

import lombok.*;
import no.fintlabs.model.CreateKafkaAclEntryResponse;
import no.fintlabs.model.CreateKafkaUserResponse;
import no.fintlabs.model.KafkaAclEntry;
import no.fintlabs.model.KafkaUser;

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
    private KafkaUser user;
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
