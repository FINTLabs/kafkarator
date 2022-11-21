package no.fintlabs.model;

import lombok.*;

import java.io.Serializable;

@Data
public class CreateKafkaUserResponse implements Serializable {
    private String message;
    private KafkaUser user;
}