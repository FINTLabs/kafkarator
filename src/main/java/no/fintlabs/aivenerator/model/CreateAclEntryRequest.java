package no.fintlabs.aivenerator.model;

import lombok.Data;

@Data
public class CreateAclEntryRequest {
    private String permission;
    private String topic;
    private String username;
}
