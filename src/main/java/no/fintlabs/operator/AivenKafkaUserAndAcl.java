package no.fintlabs.operator;

import lombok.*;
import no.fintlabs.model.CreateAclEntryResponse;
import no.fintlabs.model.CreateUserResponse;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AivenKafkaUserAndAcl {
    private CreateUserResponse user;
    private CreateAclEntryResponse.ACL acl;
}
