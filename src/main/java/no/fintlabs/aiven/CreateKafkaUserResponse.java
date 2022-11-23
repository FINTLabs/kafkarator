package no.fintlabs.aiven;

import lombok.*;

import java.io.Serializable;

@Data
public class CreateKafkaUserResponse implements Serializable {
    private String message;
    private AivenServiceUser user;
}