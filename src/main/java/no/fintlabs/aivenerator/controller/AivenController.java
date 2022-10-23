package no.fintlabs.aivenerator.controller;

import no.fintlabs.aivenerator.model.CreateUserRequest;
import no.fintlabs.aivenerator.model.CreateUserResponse;
import no.fintlabs.aivenerator.service.AivenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/")
@RestController
public class AivenController {

    private final AivenService aivenService;

    public AivenController(AivenService aivenService) {
        this.aivenService = aivenService;
    }

    @PostMapping("/project/{project}/service/{service_name}/user")
    public ResponseEntity<Void> createUserForService(@PathVariable String project, @PathVariable String service_name, @RequestBody CreateUserRequest request) {
        CreateUserResponse response = aivenService.createUserForService(project, service_name, request.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
