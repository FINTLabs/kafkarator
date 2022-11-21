package no.fintlabs.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateKafkaUserRequest {
    private String username;
}
