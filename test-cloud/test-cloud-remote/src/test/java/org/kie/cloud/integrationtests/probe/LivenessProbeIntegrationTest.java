/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.cloud.integrationtests.probe;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.kie.cloud.api.DeploymentScenarioBuilderFactory;
import org.kie.cloud.api.DeploymentScenarioBuilderFactoryLoader;
import org.kie.cloud.api.deployment.Instance;
import org.kie.cloud.api.deployment.KieServerDeployment;
import org.kie.cloud.api.deployment.WorkbenchDeployment;
import org.kie.cloud.api.scenario.WorkbenchKieServerDatabaseScenario;
import org.kie.cloud.api.scenario.WorkbenchKieServerScenario;
import org.kie.cloud.integrationtests.AbstractCloudIntegrationTest;
import org.kie.cloud.integrationtests.util.TimeUtils;
import org.kie.cloud.maven.constants.MavenConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
public class LivenessProbeIntegrationTest extends AbstractCloudIntegrationTest<WorkbenchKieServerScenario> {

    @Parameter(value = 0)
    public String testScenarioName;

    @Parameter(value = 1)
    public WorkbenchKieServerScenario workbenchKieServerScenario;

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        DeploymentScenarioBuilderFactory deploymentScenarioFactory = DeploymentScenarioBuilderFactoryLoader.getInstance();

        WorkbenchKieServerScenario workbenchKieServerScenario = deploymentScenarioFactory.getWorkbenchKieServerScenarioBuilder()
                .withExternalMavenRepo(MavenConstants.getMavenRepoUrl(), MavenConstants.getMavenRepoUser(), MavenConstants.getMavenRepoPassword())
                .build();
        WorkbenchKieServerDatabaseScenario workbenchKieServerDatabaseScenario = deploymentScenarioFactory.getWorkbenchKieServerDatabaseScenarioBuilder()
                .withExternalMavenRepo(MavenConstants.getMavenRepoUrl(), MavenConstants.getMavenRepoUser(), MavenConstants.getMavenRepoPassword())
                .build();

        return Arrays.asList(new Object[][]{
            {"Workbench + KIE Server", workbenchKieServerScenario},
            {"Workbench + KIE Server + Database", workbenchKieServerDatabaseScenario},
        });
    }

    @Override
    protected WorkbenchKieServerScenario createDeploymentScenario(DeploymentScenarioBuilderFactory deploymentScenarioFactory) {
        return workbenchKieServerScenario;
    }

    private static final String UNDEPLOY_COMMAND = "/opt/eap/bin/jboss-cli.sh -c --command='undeploy ROOT.war'";
    private static final Duration KILL_POD_TIME = Duration.ofSeconds(60);
    private static final Logger logger = LoggerFactory.getLogger(LivenessProbeIntegrationTest.class);

    @Test
    @Ignore
    public void testWorkbenchLivenessProbe() {
        WorkbenchDeployment workbenchDeployment = deploymentScenario.getWorkbenchDeployment();
        Instance workbenchInstance = workbenchDeployment.getInstances().get(0);
        String brokenPodName = workbenchInstance.getName();
        logger.info("Running undepoloy command '{}' for workbench", UNDEPLOY_COMMAND);
        workbenchInstance.runCommand("/bin/bash", "-c", UNDEPLOY_COMMAND);

        logger.info("Waiting for liveness probe to kill workbench");
        TimeUtils.wait(KILL_POD_TIME,
                () -> workbenchDeployment.getInstances().stream()
                .noneMatch(instance -> instance.getName().equals(brokenPodName))
        );
        workbenchDeployment.waitForScale();

        workbenchInstance = workbenchDeployment.getInstances().get(0);
        String newPodName = workbenchInstance.getName();

        Assertions.assertThat(newPodName).isNotEqualTo(brokenPodName);
    }

    @Test
    @Ignore
    public void testKieServerLivenessProbe() {
        KieServerDeployment kieServerDeployment = deploymentScenario.getKieServerDeployment();
        Instance kieServerInstance = kieServerDeployment.getInstances().get(0);
        String brokenPodName = kieServerInstance.getName();
        logger.info("Running undepoloy command '{}' for workbench", UNDEPLOY_COMMAND);
        kieServerInstance.runCommand("/bin/bash", "-c", UNDEPLOY_COMMAND);

        logger.info("Waiting for liveness probe to kill kie server");
        TimeUtils.wait(KILL_POD_TIME,
                () -> kieServerDeployment.getInstances().stream()
                .noneMatch(instance -> instance.getName().equals(brokenPodName))
        );
        kieServerDeployment.waitForScale();

        kieServerInstance = kieServerDeployment.getInstances().get(0);
        String newPodName = kieServerInstance.getName();

        Assertions.assertThat(newPodName).isNotEqualTo(brokenPodName);
    }

}
