package no.fintlabs.aiven;

import lombok.*;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"username"})
public class AivenServiceUser {
    private String access_cert;
    private String access_key;
    private String password;
    private String type;
    private String username;

    public static AivenServiceUser fromUsername(String username) {
        AivenServiceUser user = new AivenServiceUser();
        user.setUsername(username);

        return user;
    }
}