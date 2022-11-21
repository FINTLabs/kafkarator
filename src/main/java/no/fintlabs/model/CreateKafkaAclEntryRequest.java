package no.fintlabs.model;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateKafkaAclEntryRequest {
    private String permission;
    private String topic;
    private String username;
}
