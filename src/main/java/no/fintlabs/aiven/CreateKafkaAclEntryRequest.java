package no.fintlabs.aiven;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
