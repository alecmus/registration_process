package com.github.alecmus.registration_process.workers;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RegistrationWorkers {

    private static Logger logger = LoggerFactory.getLogger(RegistrationWorkers.class);

    @JobWorker(type = "SendVerificationEmail")
    public void sendVerificationEmail(final ActivatedJob job) {
        String email = (String)job.getVariablesAsMap().get("emailAddress");
        logger.info("Worker invoked - SendVerificationEmail [" + email + "]");
    }
}
