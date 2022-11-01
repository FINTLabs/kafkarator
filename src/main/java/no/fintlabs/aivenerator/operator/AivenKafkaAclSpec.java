package no.fintlabs.aivenerator.operator;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AivenKafkaAclSpec {
    private String project;
    private String service;
    private Acl acl;

    @Data
    public class Acl {
        private String topic;
        private String permission;
    }
}
