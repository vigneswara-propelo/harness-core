/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.telemetry.helpers;

import static io.harness.rule.OwnerRule.TMACARI;
import static io.harness.telemetry.helpers.DeploymentsInstrumentationHelper.STEP_EXECUTION_EVENT_NAME_PREFIX;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
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

public class DeploymentsInstrumentationHelperTest extends CategoryTest {
  @InjectMocks DeploymentsInstrumentationHelper instrumentationHelper;
  @Mock TelemetryReporter telemetryReporter;
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    Reflect.on(instrumentationHelper).set("telemetryReporter", telemetryReporter);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testPublishStepEvent() {
    Ambiance ambiance = getAmbiance();
    ArgumentCaptor<HashMap<String, Object>> captor = ArgumentCaptor.forClass(HashMap.class);

    CompletableFuture<Void> future = instrumentationHelper.publishStepEvent(
        ambiance, StepExecutionTelemetryEventDTO.builder().stepType("stepType").build());
    future.join();
    verify(telemetryReporter)
        .sendTrackEvent(eq(STEP_EXECUTION_EVENT_NAME_PREFIX + "stepType"), anyString(), eq("test-account"),
            captor.capture(), any(), any(), any());

    HashMap<String, Object> propertiesMap = captor.getValue();
    assertThat(propertiesMap.get(DeploymentsInstrumentationHelper.STAGE_EXECUTION_ID)).isEqualTo("stageExecutionId");
    assertThat(propertiesMap.get(DeploymentsInstrumentationHelper.PIPELINE_EXECUTION_ID)).isEqualTo("planExecutionId");
    assertThat(propertiesMap.get(DeploymentsInstrumentationHelper.ORG_ID)).isEqualTo("test-org");
    assertThat(propertiesMap.get(DeploymentsInstrumentationHelper.PROJECT_ID)).isEqualTo("test-project");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testPublishStepEventSendNoStepType() {
    Ambiance ambiance = getAmbiance();

    instrumentationHelper.publishStepEvent(ambiance, StepExecutionTelemetryEventDTO.builder().build());
    verify(telemetryReporter, times(0))
        .sendTrackEvent(any(), anyString(), eq("test-account"), any(), any(), any(), any());
  }

  private Ambiance getAmbiance() {
    return Ambiance.newBuilder()
        .putSetupAbstractions("accountId", "test-account")
        .putSetupAbstractions("projectIdentifier", "test-project")
        .putSetupAbstractions("orgIdentifier", "test-org")
        .setStageExecutionId("stageExecutionId")
        .setPlanExecutionId("planExecutionId")
        .build();
  }
}
