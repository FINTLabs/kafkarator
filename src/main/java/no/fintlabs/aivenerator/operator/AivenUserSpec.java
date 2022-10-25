package no.fintlabs.aivenerator.operator;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AivenUserSpec {
    private String project;
    private String service;
}
