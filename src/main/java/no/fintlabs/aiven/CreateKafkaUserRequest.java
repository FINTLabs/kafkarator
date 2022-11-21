package no.fintlabs.aiven;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateKafkaUserRequest {
    private String username;
}
