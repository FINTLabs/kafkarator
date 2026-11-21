package no.fintlabs.aiven;

import lombok.Data;

import java.io.Serializable;

@Data
public class GetKafkaUserResponse implements Serializable {
    private AivenServiceUser user;
}
