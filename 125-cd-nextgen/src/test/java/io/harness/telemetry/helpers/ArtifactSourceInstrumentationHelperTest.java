/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.telemetry.helpers;

import static io.harness.rule.OwnerRule.RAKSHIT_AGARWAL;
import static io.harness.rule.OwnerRule.SARTHAK_KASAT;
import static io.harness.rule.OwnerRule.vivekveman;
import static io.harness.telemetry.helpers.InstrumentationConstants.ACCOUNT;
import static io.harness.telemetry.helpers.InstrumentationConstants.API_TYPE;
import static io.harness.telemetry.helpers.InstrumentationConstants.ARTIFACT_IDENTIFIER;
import static io.harness.telemetry.helpers.InstrumentationConstants.ARTIFACT_TYPE;
import static io.harness.telemetry.helpers.InstrumentationConstants.DEPLOYMENT_TYPE;
import static io.harness.telemetry.helpers.InstrumentationConstants.IS_ARTIFACT_PRIMARY;
import static io.harness.telemetry.helpers.InstrumentationConstants.IS_SERVICE_V2;
import static io.harness.telemetry.helpers.InstrumentationConstants.ORG;
import static io.harness.telemetry.helpers.InstrumentationConstants.PROJECT;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.telemetry.TelemetryReporter;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import org.jooq.tools.reflect.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ArtifactSourceInstrumentationHelperTest extends CategoryTest {
  @InjectMocks ArtifactSourceInstrumentationHelper instrumentationHelper;
  @Mock TelemetryReporter telemetryReporter;
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    Reflect.on(instrumentationHelper).set("telemetryReporter", telemetryReporter);
  }

  @Test
  @Owner(developers = SARTHAK_KASAT)
  @Category(UnitTests.class)
  public void testLastPublishedTagTrackSend() {
    DockerHubArtifactConfig dockerHubArtifactConfig =
        DockerHubArtifactConfig.builder().imagePath(ParameterField.createValueField("IMAGE")).build();
    CompletableFuture<Void> telemetryTask =
        instrumentationHelper.sendLastPublishedTagExpressionEvent(dockerHubArtifactConfig, ACCOUNT, ORG, PROJECT);
    telemetryTask.join();
    assertTrue(telemetryTask.isDone());
  }
  @Test
  @Owner(developers = vivekveman)
  @Category(UnitTests.class)
  public void testArtifactDeploymentTrack() {
    DockerHubArtifactConfig dockerHubArtifactConfig = DockerHubArtifactConfig.builder()
                                                          .imagePath(ParameterField.createValueField("IMAGE"))
                                                          .identifier("identifier")
                                                          .build();
    CompletableFuture<Void> telemetryTask =
        instrumentationHelper.sendArtifactDeploymentEvent(dockerHubArtifactConfig, ACCOUNT, ORG, PROJECT, "ssh", true);
    ArgumentCaptor<HashMap> captor = ArgumentCaptor.forClass(HashMap.class);
    telemetryTask.join();
    verify(telemetryReporter, times(1)).sendTrackEvent(any(), any(), any(), captor.capture(), any(), any(), any());
    HashMap<String, Object> eventPropertiesMap = captor.getValue();
    assert (eventPropertiesMap.get(ACCOUNT)).equals("account");
    assert (eventPropertiesMap.get(ORG)).equals("org");
    assert (eventPropertiesMap.get(ARTIFACT_TYPE)).equals(ArtifactSourceType.DOCKER_REGISTRY);
    assert (eventPropertiesMap.get(ARTIFACT_IDENTIFIER)).equals("identifier");
    assert (eventPropertiesMap.get(IS_ARTIFACT_PRIMARY)).equals(false);
    assert (eventPropertiesMap.get(PROJECT)).equals("project");
    assert (eventPropertiesMap.get(DEPLOYMENT_TYPE)).equals("ssh");
    assert (eventPropertiesMap.get(IS_SERVICE_V2)).equals(true);
    assertTrue(telemetryTask.isDone());
  }

  @Test
  @Owner(developers = RAKSHIT_AGARWAL)
  @Category(UnitTests.class)
  public void testArtifactApiEvent() {
    CompletableFuture<Void> telemetryTask = instrumentationHelper.sendArtifactApiEvent(
        ArtifactSourceType.GOOGLE_ARTIFACT_REGISTRY, ACCOUNT, ORG, PROJECT, API_TYPE, 1200, 10, false, true);
    telemetryTask.join();
    assertTrue(telemetryTask.isDone());
  }
}
