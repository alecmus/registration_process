package com.github.alecmus.registration_process;

import com.github.alecmus.registration_process.controllers.RegistrationController;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;
import io.camunda.zeebe.process.test.inspections.model.InspectedProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

@ZeebeProcessTest
class RegistrationProcessApplicationTests {

    @Autowired
    ZeebeTestEngine zeebeTestEngine;

    @Autowired
    ZeebeClient zeebeClient;

    RegistrationController registrationController;

    @BeforeEach
    public void setup() {
        // create RegistrationController instance for testing purposes
        registrationController = new RegistrationController(zeebeClient);

        // deploy form
        DeploymentEvent formDeploymentEvent = zeebeClient.newDeployResourceCommand()
                .addResourceFromClasspath("registration-form.form")
                .send()
                .join();

        // check if form deployment was successful
        BpmnAssert.assertThat(formDeploymentEvent)
                .extractingFormByFormId("Form_Registration")
                .hasFormId("Form_Registration");

        // deploy process
        DeploymentEvent processDeploymentEvent = zeebeClient.newDeployResourceCommand()
                .addResourceFromClasspath("registration-process.bpmn")
                .send()
                .join();

        // check if process deployment was successful
        BpmnAssert.assertThat(processDeploymentEvent)
                .containsProcessesByBpmnProcessId("Process_RegistrationProcess");
    }

    @Test
    public void testDeployment() {
        /*
         * Since @BeforeEach is called for each test, this test will only pass if the setup()
         * is successful, i.e., if the deployment is successful.
         */
    }

    @Test
    public void testRegisterCustomerProcess() throws Exception {
        // create process instance and capture the process instance key
        long processInstanceKey = registrationController.createProcessInstance();

        // retrieve process instance started because of the above call
        InspectedProcessInstance inspectedProcessInstance = new InspectedProcessInstance(processInstanceKey);

        // verify that the process has started
        BpmnAssert.assertThat(inspectedProcessInstance)
                .isStarted();

        // verify that the process is waiting for the user task
        BpmnAssert.assertThat(inspectedProcessInstance)
                .isWaitingAtElements("UserTask_GetRegistrationDetails");

        // complete the user task
        completeUserTask(1, Map.of());

        // verify that the process is waiting for the send email service task
        BpmnAssert.assertThat(inspectedProcessInstance)
                .isWaitingAtElements("Activity_SendVerificationEmail");

        // complete the send email service task
        completeJob("SendVerificationEmail", 1, Map.of());

        // verify that the process has passed the send email service task and is now complete
        BpmnAssert.assertThat(inspectedProcessInstance)
                .hasPassedElement("Activity_SendVerificationEmail")
                .isCompleted();
    }

    public void completeUserTask(int count, Map<String, Object> variables) throws Exception {
        ActivateJobsResponse activateJobsResponse = zeebeClient.newActivateJobsCommand()
                .jobType("io.camunda.zeebe:userTask")
                .maxJobsToActivate(count)
                .send().join();

        List<ActivatedJob> activatedJobs = activateJobsResponse.getJobs();

        if (activatedJobs.size() != count) {
            fail("No user task found");
        }

        for (ActivatedJob job : activatedJobs) {
            zeebeClient.newCompleteCommand(job)
                    .variables(variables)
                    .send().join();
        }

        // allow a bit of time for engine to be idle
        zeebeTestEngine.waitForIdleState(Duration.ofSeconds(1));
    }

    // complete job using a job handler
    public void completeJob(String jobType, int count, JobHandler handler) throws Exception {
        ActivateJobsResponse activateJobsResponse = zeebeClient.newActivateJobsCommand()
                .jobType(jobType)
                .maxJobsToActivate(count)
                .send().join();

        List<ActivatedJob> activatedJobs = activateJobsResponse.getJobs();

        if (activatedJobs.size() != count) {
            fail("No user task found");
        }

        for (ActivatedJob job : activatedJobs) {
            handler.handle(zeebeClient, job);
        }

        // allow a bit of time for engine to be idle
        zeebeTestEngine.waitForIdleState(Duration.ofSeconds(1));
    }

    // just complete the job
    public void completeJob(String jobType, int count, Map<String, Object> variables) throws Exception {
        ActivateJobsResponse activateJobsResponse = zeebeClient.newActivateJobsCommand()
                .jobType(jobType)
                .maxJobsToActivate(count)
                .send().join();

        List<ActivatedJob> activatedJobs = activateJobsResponse.getJobs();

        if (activatedJobs.size() != count) {
            fail("No user task found");
        }

        for (ActivatedJob job : activatedJobs) {
            zeebeClient.newCompleteCommand(job)
                    .variables(variables)
                    .send().join();
        }

        // allow a bit of time for engine to be idle
        zeebeTestEngine.waitForIdleState(Duration.ofSeconds(1));
    }
}
