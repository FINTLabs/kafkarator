package no.fintlabs.operator;

import lombok.*;
import no.fintlabs.model.Acl;
import no.fintlabs.model.CreateUserResponse;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AivenKafkaUserAndAcl {
    private CreateUserResponse user;
    private List<Acl> acls;
}
