package com.github.alecmus.registration_process;

import io.camunda.zeebe.spring.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Deployment(resources = {"classpath*:*.form", "classpath*:*.bpmn"})
public class RegistrationProcessApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegistrationProcessApplication.class, args);
    }

}
