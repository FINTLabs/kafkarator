package no.fintlabs.aivenerator.operator;

import lombok.*;
import no.fintlabs.aivenerator.model.CreateAclEntryResponse;
import no.fintlabs.aivenerator.model.CreateUserResponse;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AivenKafkaUserAndAcl {
    private CreateUserResponse user;
    private CreateAclEntryResponse.ACL acl;
}
