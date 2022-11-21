package no.fintlabs.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class CreateKafkaAclEntryResponse implements Serializable {
    @JsonProperty("acl")
    private List<KafkaAclEntry> kafkaAclEntry = new ArrayList<>();
    private String message;
    private boolean success;

    public KafkaAclEntry getAclByUsernameAndTopic(String username, String topic) {
        return kafkaAclEntry.stream()
                .filter(acl -> acl.getUsername().equals(username) && acl.getTopic().equals(topic))
                .findFirst()
                .orElse(null);
    }
}
