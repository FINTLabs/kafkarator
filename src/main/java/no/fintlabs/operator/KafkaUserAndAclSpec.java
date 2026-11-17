package no.fintlabs.operator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.fintlabs.FlaisSpec;
import no.fintlabs.aiven.KafkaAclEntry;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaUserAndAclSpec implements FlaisSpec {
    @Builder.Default
    private List<Acl> acls = new ArrayList<>();

    @Data
    public static class Acl {
        private String topic;
        private String permission;

        public KafkaAclEntry toAclEntry(String username) {
            KafkaAclEntry kafkaAclEntry = new KafkaAclEntry();
            kafkaAclEntry.setTopic(this.getTopic());
            kafkaAclEntry.setPermission(this.getPermission());
            kafkaAclEntry.setUsername(username);

            return kafkaAclEntry;
        }
    }
}
