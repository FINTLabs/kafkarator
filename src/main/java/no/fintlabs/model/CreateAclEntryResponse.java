package no.fintlabs.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateAclEntryResponse implements Serializable {
    private ACL[] acls;
    private String message;
    private boolean success;

    public ACL getAcl(String username) {
        for (ACL acl : acls) {
            if (acl.getUsername().equals(username)) {
                return acl;
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
