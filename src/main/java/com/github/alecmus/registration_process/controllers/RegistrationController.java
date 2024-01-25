package com.github.alecmus.registration_process.controllers;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class RegistrationController {
    private ZeebeClient zeebeClient;
    private static Logger logger = LoggerFactory.getLogger(RegistrationController.class);

    public RegistrationController(ZeebeClient zeebeClient) {
        this.zeebeClient = zeebeClient;
    }

    @PutMapping("/registration")
    public Mono<ResponseEntity<Void>> registration() {
        logger.info("Registration request received at /registration");
        logger.info("Starting process instance ...");

        // start process instance
        createProcessInstance();

        return Mono.just(ResponseEntity.accepted().build());
    }

    public long createProcessInstance() {
        ProcessInstanceEvent processInstanceEvent = zeebeClient.newCreateInstanceCommand()
                .bpmnProcessId("Process_RegistrationProcess")
                .latestVersion()
                .send().join();
        return processInstanceEvent.getProcessInstanceKey();
    }
}
