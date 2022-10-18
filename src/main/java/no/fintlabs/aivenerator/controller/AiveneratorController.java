package no.fintlabs.aivenerator.controller;


import no.fintlabs.aivenerator.service.AivenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/")
@RestController
public class AiveneratorController {

    private final AivenService aivenService;

    public AiveneratorController(AivenService aivenService){
        this.aivenService = aivenService;
    }

    @GetMapping
    public String index(){
        return "init spring boot";
    }
}
