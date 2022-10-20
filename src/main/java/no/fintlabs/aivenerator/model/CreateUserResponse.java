package no.fintlabs.aivenerator.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateUserResponse implements Serializable {
    private String accessCert;
    private String accessKey;
    private String password;
    private String type;
    private String username;
}
