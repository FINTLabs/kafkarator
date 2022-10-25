package no.fintlabs.aivenerator.controller;

import no.fintlabs.aivenerator.model.CreateAclEntryRequest;
import no.fintlabs.aivenerator.model.CreateAclEntryResponse;
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

    @DeleteMapping("/project/{project}/service/{service_name}/user/{username}")
    public ResponseEntity<Void> deleteUserForService(@PathVariable String project, @PathVariable String service_name, @PathVariable String username) {

        aivenService.deleteUserForService(project, service_name, username);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/project/{project}/service/{service_name}/acl")
    public ResponseEntity<String> createAclEntryForService(@PathVariable String project, @PathVariable String service_name, @RequestBody CreateAclEntryRequest request) {
        CreateAclEntryResponse response = aivenService.createAclEntryForTopic(project, service_name, request.getTopic(), request.getUsername(), request.getPermission());
        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response.getAcl()[response.getAcl().length - 1].getId());
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.getMessage());
        }
    }

    @DeleteMapping("/project/{project}/service/{service_name}/acl/{acl_id}")
    public ResponseEntity<Void> deleteAclEntryForService(@PathVariable String project, @PathVariable String service_name, @PathVariable String acl_id) {
        aivenService.deleteAclEntryForService(project, service_name, acl_id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
