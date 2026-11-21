package no.fintlabs.operator;

import lombok.*;
import no.fintlabs.aiven.AivenServiceUser;
import no.fintlabs.aiven.GetKafkaAclEntryResponse;
import no.fintlabs.aiven.GetKafkaUserResponse;
import no.fintlabs.aiven.KafkaAclEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    public static KafkaUserAndAcl fromUserAndAclResponse(GetKafkaUserResponse user, GetKafkaAclEntryResponse acl) {
        String username = user.getUser().getUsername();

        return KafkaUserAndAcl.builder()
                .user(user.getUser())
                .aclEntries(
                        Optional.ofNullable(acl.getKafkaAclEntry())
                                .orElseGet(ArrayList::new)
                                .stream()
                                .filter(kafkaAclEntry ->
                                        Objects.equals(kafkaAclEntry.getUsername(), username)
                                )
                                .collect(Collectors.toList())
                )
                .build();
    }
}
