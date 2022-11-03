package no.fintlabs.aivenerator.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateAclEntryResponse implements Serializable {
    private ACL[] acls;
    private String message;
    private boolean success;

    @Data
    public static class ACL {
        private String id;
        private String permission;
        private String topic;
        private String username;
    }
}
