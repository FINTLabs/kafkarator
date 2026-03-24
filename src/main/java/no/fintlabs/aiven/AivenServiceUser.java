package no.fintlabs.aiven;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"username"})
public class AivenServiceUser {
    @JsonProperty("access_cert")
    private String accessCert;
    @JsonProperty("access_key")
    private String accessKey;
    private String password;
    private String type;
    private String username;

    public static AivenServiceUser fromUsername(String username) {
        AivenServiceUser user = new AivenServiceUser();
        user.setUsername(username);

        return user;
    }
}