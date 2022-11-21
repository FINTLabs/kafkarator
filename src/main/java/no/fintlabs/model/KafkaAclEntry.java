package no.fintlabs.model;

import lombok.*;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"id"})
public class KafkaAclEntry {
    private String id;
    private String permission;
    private String topic;
    private String username;
}
