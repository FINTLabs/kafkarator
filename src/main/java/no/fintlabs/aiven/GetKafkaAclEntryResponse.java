package no.fintlabs.aiven;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class GetKafkaAclEntryResponse implements Serializable {
    @JsonProperty("acl")
    private List<KafkaAclEntry> kafkaAclEntry = new ArrayList<>();

    public KafkaAclEntry getAclByUsernameAndTopic(String username, String topic) {
        return kafkaAclEntry.stream()
                .filter(acl -> acl.getUsername().equals(username) && acl.getTopic().equals(topic))
                .findFirst()
                .orElse(null);
    }
}
