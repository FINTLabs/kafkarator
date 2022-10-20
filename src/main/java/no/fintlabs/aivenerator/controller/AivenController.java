package no.fintlabs.aivenerator.controller;

import no.fintlabs.aivenerator.model.CreateUserRequest;
import no.fintlabs.aivenerator.model.CreateUserResponse;
import no.fintlabs.aivenerator.service.AivenService;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/")
@RestController
public class AivenController {

    private final AivenService aivenService;

    public AivenController(AivenService aivenService) {
        this.aivenService = aivenService;
    }

    @PostMapping("/project/{project}/service/{service_name}/user")
    public String createUserForService(@PathVariable String project, @PathVariable String service_name, @RequestBody CreateUserRequest request) {
        CreateUserResponse response = aivenService.createUserForService(project, service_name, request.getUsername());

        return response.getUser().getUsername() + " created for service " + service_name;
    }
}
