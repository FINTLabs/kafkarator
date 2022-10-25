package no.fintlabs.aivenerator.operator;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AivenUserStatus extends ObservedGenerationAwareStatus {
    private String errorMessage;
}
