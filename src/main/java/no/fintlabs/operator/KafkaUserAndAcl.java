package no.fintlabs.operator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.fintlabs.aiven.AivenServiceUser;
import no.fintlabs.aiven.CreateKafkaAclEntryResponse;
import no.fintlabs.aiven.CreateKafkaUserResponse;
import no.fintlabs.aiven.KafkaAclEntry;

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
    @Builder.Default
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
