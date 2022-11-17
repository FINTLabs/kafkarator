package no.fintlabs.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CreateAclEntryResponse implements Serializable {
    private List<Acl> acl;
    private String message;
    private boolean success;

    public Acl getAclByUsernameAndTopic(String username, String topic) {
        return acl.stream()
                .filter(acl -> acl.getUsername().equals(username) && acl.getTopic().equals(topic))
                .findFirst()
                .orElse(null);
    }
}
