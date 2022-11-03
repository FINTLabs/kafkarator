package no.fintlabs.operator;

import lombok.*;
import no.fintlabs.FlaisSpec;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AivenKafkaAclSpec implements FlaisSpec {
    private String project;
    private String service;
    private Acl acl;

    @Data
    public static class Acl {
        private String topic;
        private String permission;
    }
}
