package no.fintlabs.model;

import lombok.Data;

@Data
public class Acl {
    private String id;
    private String permission;
    private String topic;
    private String username;
}
