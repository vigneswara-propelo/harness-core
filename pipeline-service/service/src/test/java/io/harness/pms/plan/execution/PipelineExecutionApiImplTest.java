/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.plan.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.BRIJESH;
import static io.harness.rule.OwnerRule.UTKARSH_CHOUBEY;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.engine.executions.retry.RetryInfo;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.plan.execution.beans.dto.RunStageRequestDTO;
import io.harness.rule.Owner;
import io.harness.spec.server.pipeline.v1.model.PipelineExecuteRequestBody;
import io.harness.spec.server.pipeline.v1.model.PipelineExecuteResponseBody;
import io.harness.spec.server.pipeline.v1.model.RerunPipelineRequestBody;
import io.harness.spec.server.pipeline.v1.model.RunStageRequestBody;
import io.harness.utils.PmsFeatureFlagService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;

@OwnedBy(PIPELINE)
@PrepareForTest({PlanExecutionUtils.class, UUIDGenerator.class})
public class PipelineExecutionApiImplTest extends CategoryTest {
  @Mock PipelineExecutor pipelineExecutor;
  @Mock PmsFeatureFlagService pmsFeatureFlagService;
  @Mock RetryExecutionHelper retryExecutionHelper;
  @InjectMocks PipelineExecutionApiImpl pipelineExecutionApi;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testPipelineCreate() {
    String accountId = generateUuid();
    String orgId = generateUuid();
    String projectId = generateUuid();
    String pipelineId = generateUuid();

    String planExecutionId = generateUuid();

    PipelineExecuteRequestBody pipelineExecuteBody = new PipelineExecuteRequestBody();
    Status status = Status.RUNNING;
    pipelineExecuteBody.setInputsYaml("inputSetYaml");
    String module = "CD";

    doReturn(PlanExecutionResponseDto.builder()
                 .planExecution(PlanExecution.builder().uuid(planExecutionId).status(status).build())
                 .build())
        .when(pipelineExecutor)
        .runPipelineWithInputSetPipelineYaml(
            accountId, orgId, projectId, pipelineId, module, pipelineExecuteBody.getInputsYaml(), false, false, "");

    Response response = pipelineExecutionApi.executePipeline(
        orgId, projectId, pipelineId, pipelineExecuteBody, accountId, module, false, false, "", null, null, null);

    PipelineExecuteResponseBody responseBody = (PipelineExecuteResponseBody) response.getEntity();

    assertThat(responseBody.getExecutionDetails().getExecutionId()).isEqualTo(planExecutionId);
    assertThat(responseBody.getExecutionDetails().getStatus()).isEqualTo(status.toString());
  }

  @Test
  @Owner(developers = UTKARSH_CHOUBEY)
  @Category(UnitTests.class)
  public void testExecuteStagesWithRuntimeInputYaml() {
    String accountId = generateUuid();
    String orgId = generateUuid();
    String projectId = generateUuid();
    String pipelineId = generateUuid();

    String planExecutionId = generateUuid();
    when(pmsFeatureFlagService.isEnabled(eq(accountId), eq(FeatureName.PIE_GET_FILE_CONTENT_ONLY))).thenReturn(false);
    RunStageRequestBody runStageRequestBody = new RunStageRequestBody();
    runStageRequestBody.setStageIdentifiers(Arrays.asList("stg1"));
    RunStageRequestDTO runStageRequestDTO = RunStageRequestDTO.builder()
                                                .stageIdentifiers(runStageRequestBody.getStageIdentifiers())
                                                .runtimeInputYaml(runStageRequestBody.getInputsYaml())
                                                .expressionValues(runStageRequestBody.getExpressionValues())
                                                .build();
    Status status = Status.RUNNING;
    String module = "CD";

    doReturn(PlanExecutionResponseDto.builder()
                 .planExecution(PlanExecution.builder().uuid(planExecutionId).status(status).build())
                 .build())
        .when(pipelineExecutor)
        .runStagesWithRuntimeInputYaml(accountId, orgId, projectId, pipelineId, module, runStageRequestDTO, false, "");

    Response response = pipelineExecutionApi.executeStagesWithInputYaml(
        orgId, projectId, pipelineId, runStageRequestBody, accountId, module, "false", "", null, null, null);

    PipelineExecuteResponseBody responseBody = (PipelineExecuteResponseBody) response.getEntity();

    assertThat(responseBody.getExecutionDetails().getExecutionId()).isEqualTo(planExecutionId);
    assertThat(responseBody.getExecutionDetails().getStatus()).isEqualTo(status.toString());
  }

  @Test
  @Owner(developers = UTKARSH_CHOUBEY)
  @Category(UnitTests.class)
  public void testRerunStagesExecutionOfPipeline() {
    String accountId = generateUuid();
    String orgId = generateUuid();
    String projectId = generateUuid();
    String pipelineId = generateUuid();

    String planExecutionId = generateUuid();
    when(pmsFeatureFlagService.isEnabled(eq(accountId), eq(FeatureName.PIE_GET_FILE_CONTENT_ONLY))).thenReturn(false);
    RunStageRequestBody runStageRequestBody = new RunStageRequestBody();
    runStageRequestBody.setStageIdentifiers(Arrays.asList("stg1"));
    RunStageRequestDTO runStageRequestDTO = RunStageRequestDTO.builder()
                                                .stageIdentifiers(runStageRequestBody.getStageIdentifiers())
                                                .runtimeInputYaml(runStageRequestBody.getInputsYaml())
                                                .expressionValues(runStageRequestBody.getExpressionValues())
                                                .build();
    Status status = Status.RUNNING;
    String module = "CD";

    doReturn(PlanExecutionResponseDto.builder()
                 .planExecution(PlanExecution.builder().uuid(planExecutionId).status(status).build())
                 .build())
        .when(pipelineExecutor)
        .rerunStagesWithRuntimeInputYaml(
            accountId, orgId, projectId, pipelineId, module, planExecutionId, runStageRequestDTO, false, false, "");

    Response response = pipelineExecutionApi.rerunStagesExecutionOfPipeline(orgId, projectId, pipelineId,
        planExecutionId, runStageRequestBody, accountId, module, "false", "", null, module, "");

    PipelineExecuteResponseBody responseBody = (PipelineExecuteResponseBody) response.getEntity();

    assertThat(responseBody.getExecutionDetails().getExecutionId()).isEqualTo(planExecutionId);
    assertThat(responseBody.getExecutionDetails().getStatus()).isEqualTo(status.toString());
  }

  @Test
  @Owner(developers = UTKARSH_CHOUBEY)
  @Category(UnitTests.class)
  public void testRetryPipelineWithInputsetPipelineYaml() {
    String accountId = generateUuid();
    String orgId = generateUuid();
    String projectId = generateUuid();
    String pipelineId = generateUuid();

    String planExecutionId = generateUuid();
    when(pmsFeatureFlagService.isEnabled(eq(accountId), eq(FeatureName.PIE_GET_FILE_CONTENT_ONLY))).thenReturn(false);
    when(retryExecutionHelper.validateRetry(accountId, orgId, projectId, pipelineId, planExecutionId, null))
        .thenReturn(RetryInfo.builder().isResumable(true).build());
    RerunPipelineRequestBody rerunPipelineRequestBody = new RerunPipelineRequestBody();
    rerunPipelineRequestBody.setInputsYaml("input");
    Status status = Status.RUNNING;
    String module = "CD";

    doReturn(PlanExecutionResponseDto.builder()
                 .planExecution(PlanExecution.builder().uuid(planExecutionId).status(status).build())
                 .build())
        .when(pipelineExecutor)
        .retryPipelineWithInputSetPipelineYaml(accountId, orgId, projectId, pipelineId, module,
            rerunPipelineRequestBody.getInputsYaml(), planExecutionId, List.of("stage1"), false, false, false, "");

    Response response = pipelineExecutionApi.retryPipelineWithInputsetPipelineYaml(orgId, projectId, pipelineId,
        planExecutionId, rerunPipelineRequestBody, accountId, module, List.of("stage1"), false, "");

    PipelineExecuteResponseBody responseBody = (PipelineExecuteResponseBody) response.getEntity();

    assertThat(responseBody.getExecutionDetails().getExecutionId()).isEqualTo(planExecutionId);
    assertThat(responseBody.getExecutionDetails().getStatus()).isEqualTo(status.toString());
  }

  @Test
  @Owner(developers = UTKARSH_CHOUBEY)
  @Category(UnitTests.class)
  public void testRerunPipeline() {
    String accountId = generateUuid();
    String orgId = generateUuid();
    String projectId = generateUuid();
    String pipelineId = generateUuid();

    String planExecutionId = generateUuid();
    when(pmsFeatureFlagService.isEnabled(eq(accountId), eq(FeatureName.PIE_GET_FILE_CONTENT_ONLY))).thenReturn(false);
    RerunPipelineRequestBody rerunPipelineRequestBody = new RerunPipelineRequestBody();
    rerunPipelineRequestBody.setInputsYaml("input");
    Status status = Status.RUNNING;
    String module = "CD";

    doReturn(PlanExecutionResponseDto.builder()
                 .planExecution(PlanExecution.builder().uuid(planExecutionId).status(status).build())
                 .build())
        .when(pipelineExecutor)
        .runPipelineWithInputSetPipelineYaml(accountId, orgId, projectId, pipelineId, module,
            rerunPipelineRequestBody.getInputsYaml(), false, false, "");

    Response response = pipelineExecutionApi.rerunPipeline(orgId, projectId, pipelineId, planExecutionId,
        rerunPipelineRequestBody, accountId, module, false, "", null, null, null);

    PipelineExecuteResponseBody responseBody = (PipelineExecuteResponseBody) response.getEntity();

    assertThat(responseBody.getExecutionDetails().getExecutionId()).isEqualTo(planExecutionId);
    assertThat(responseBody.getExecutionDetails().getStatus()).isEqualTo(status.toString());
  }
}
