package no.fintlabs.aivenerator.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateUserResponse implements Serializable {
    private String message;
    private User user;

    @Data
    public static class User {
        private String access_cert;
        private String access_key;
        private String password;
        private String type;
        private String username;
    }
}