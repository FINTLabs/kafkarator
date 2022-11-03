package no.fintlabs.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateAclEntryResponse implements Serializable {
    private ACL[] acl;
    private String message;
    private boolean success;

    public ACL getAclByUsername(String username) {
        for (ACL currentAcl : acl) {
            if (currentAcl.getUsername().equals(username)) {
                return currentAcl;
            }
        }
        return null;
    }

    @Data
    public static class ACL {
        private String id;
        private String permission;
        private String topic;
        private String username;
    }
}
