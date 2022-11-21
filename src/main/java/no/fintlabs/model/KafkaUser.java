package no.fintlabs.model;

import lombok.*;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"username"})
public class KafkaUser {
    private String access_cert;
    private String access_key;
    private String password;
    private String type;
    private String username;

    public static KafkaUser fromUsername(String username) {
        KafkaUser user = new KafkaUser();
        user.setUsername(username);

        return user;
    }
}