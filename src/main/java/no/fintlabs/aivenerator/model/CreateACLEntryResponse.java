package no.fintlabs.aivenerator.model;

import lombok.Data;

@Data
public class CreateACLEntryResponse {
    private ACL acl;
    private String message;
    private boolean success;

    public static class ACL {
        private String id;
        private String permission;
        private String topic;
        private String username;
    }
}
