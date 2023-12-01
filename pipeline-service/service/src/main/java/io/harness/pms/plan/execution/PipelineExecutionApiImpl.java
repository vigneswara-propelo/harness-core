/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.plan.execution;

import static io.harness.beans.FeatureName.PIE_GET_FILE_CONTENT_ONLY;

import static java.lang.String.format;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.FeatureName;
import io.harness.engine.executions.retry.RetryInfo;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.ngexception.NGTemplateException;
import io.harness.exception.ngexception.PipelineException;
import io.harness.gitaware.helper.GitAwareContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitx.USER_FLOW;
import io.harness.pms.annotations.PipelineServiceAuth;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.mappers.GitXCacheMapper;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSPipelineTemplateHelper;
import io.harness.pms.pipeline.yaml.BasicPipeline;
import io.harness.pms.plan.execution.beans.dto.RunStageRequestDTO;
import io.harness.pms.stages.StageExecutionResponse;
import io.harness.pms.stages.StageExecutionSelectorHelper;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.pms.yaml.YamlUtils;
import io.harness.spec.server.pipeline.v1.PipelineExecutionApi;
import io.harness.spec.server.pipeline.v1.model.ExecutionDetails;
import io.harness.spec.server.pipeline.v1.model.PipelineExecuteBody;
import io.harness.spec.server.pipeline.v1.model.PipelineExecuteResponseBody;
import io.harness.spec.server.pipeline.v1.model.RerunPipelineRequestBody;
import io.harness.spec.server.pipeline.v1.model.RunStageRequestBody;
import io.harness.spec.server.pipeline.v1.model.StageExecutionResponseBody;
import io.harness.spec.server.pipeline.v1.model.StageExecutionResponseList;
import io.harness.utils.PmsFeatureFlagService;
import io.harness.utils.ThreadOperationContextHelper;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(HarnessTeam.PIPELINE)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@PipelineServiceAuth
@Slf4j
public class PipelineExecutionApiImpl implements PipelineExecutionApi {
  private final PipelineExecutor pipelineExecutor;
  private final PmsFeatureFlagService pmsFeatureFlagService;
  private final RetryExecutionHelper retryExecutionHelper;
  private final PMSPipelineService pmsPipelineService;
  private final PMSPipelineTemplateHelper pipelineTemplateHelper;

  @Override
  public Response executePipeline(String org, String project, String pipeline, @Valid PipelineExecuteBody body,
      String harnessAccount, String module, Boolean useFqnIfErrorResponse, Boolean notifyOnlyUser, String notes) {
    try {
      String inputSetPipelineYaml = null;
      if (body != null) {
        inputSetPipelineYaml = body.getYaml();
      }
      PlanExecutionResponseDto planExecutionResponseDto = pipelineExecutor.runPipelineWithInputSetPipelineYaml(
          harnessAccount, org, project, pipeline, module, inputSetPipelineYaml, false, notifyOnlyUser, notes);
      PipelineExecuteResponseBody pipelineExecuteResponseBody =
          getPipelineExecutionResponseFromPlanExecutionResponse(planExecutionResponseDto);
      return Response.ok().entity(pipelineExecuteResponseBody).build();
    } catch (NGTemplateException ex) {
      throw new PipelineException(
          PipelineException.PIPELINE_Execution_MESSAGE, ex, ErrorCode.NG_PIPELINE_EXECUTION_EXCEPTION);
    }
  }

  @Override
  public Response executeStagesWithInputYaml(String org, String project, String pipeline,
      @Valid RunStageRequestBody body, String harnessAccount, String module, String useFqnIfError, String notes,
      String branchName, String connectorRef, String repoName) {
    if (pmsFeatureFlagService.isEnabled(harnessAccount, PIE_GET_FILE_CONTENT_ONLY)) {
      ThreadOperationContextHelper.setUserFlow(USER_FLOW.EXECUTION);
    }
    RunStageRequestDTO runStageRequestDTO = getRunStageRequestDTO(body);
    PlanExecutionResponseDto planExecutionResponseDto = pipelineExecutor.runStagesWithRuntimeInputYaml(
        harnessAccount, org, project, pipeline, module, runStageRequestDTO, false, notes);
    PipelineExecuteResponseBody pipelineExecuteResponseBody =
        getPipelineExecutionResponseFromPlanExecutionResponse(planExecutionResponseDto);
    return Response.ok().entity(pipelineExecuteResponseBody).build();
  }

  private RunStageRequestDTO getRunStageRequestDTO(RunStageRequestBody body) {
    return RunStageRequestDTO.builder()
        .stageIdentifiers(body.getStageIdentifiers())
        .runtimeInputYaml(body.getInputsYaml())
        .expressionValues(body.getExpressionValues())
        .build();
  }

  @Override
  public Response getStagesExecutionList(String org, String project, String pipeline, String harnessAccount,
      String loadFromCache, String branchName, String connectorRef, String repoName) {
    if (pmsFeatureFlagService.isEnabled(harnessAccount, PIE_GET_FILE_CONTENT_ONLY)) {
      ThreadOperationContextHelper.setUserFlow(USER_FLOW.EXECUTION);
    }
    Optional<PipelineEntity> optionalPipelineEntity = pmsPipelineService.getPipeline(harnessAccount, org, project,
        pipeline, false, false, false, GitXCacheMapper.parseLoadFromCacheHeaderParam(loadFromCache));
    if (!optionalPipelineEntity.isPresent()) {
      throw new InvalidRequestException(
          format("Pipeline [%s] under Project[%s], Organization [%s] doesn't exist.", pipeline, project, org));
    }
    PipelineEntity pipelineEntity = optionalPipelineEntity.get();
    String yaml = pipelineEntity.getYaml();
    if (Boolean.TRUE.equals(pipelineEntity.getTemplateReference())) {
      yaml = pipelineTemplateHelper
                 .resolveTemplateRefsInPipeline(harnessAccount, org, project, pipelineEntity.getYaml(), loadFromCache,
                     pipelineEntity.getHarnessVersion())
                 .getMergedPipelineYaml();
    }
    boolean shouldAllowStageExecutions;
    if (HarnessYamlVersion.V0.equals(pipelineEntity.getHarnessVersion())) {
      try {
        BasicPipeline basicPipeline = YamlUtils.read(yaml, BasicPipeline.class);
        shouldAllowStageExecutions = basicPipeline.isAllowStageExecutions();
      } catch (IOException e) {
        throw new InvalidRequestException("Cannot create pipeline entity due to " + e.getMessage(), e);
      }
    } else {
      shouldAllowStageExecutions = true;
    }

    if (!shouldAllowStageExecutions) {
      return Response.ok().entity(Collections.emptyList()).build();
    }
    boolean isYamlSimplificationEnabled =
        pmsFeatureFlagService.isEnabled(harnessAccount, FeatureName.CI_YAML_VERSIONING);
    List<StageExecutionResponse> stageExecutionResponse =
        StageExecutionSelectorHelper.getStageExecutionResponse(yaml, isYamlSimplificationEnabled);
    List<StageExecutionResponseBody> stagesExecutions = getStageExecutionResponse(stageExecutionResponse);
    StageExecutionResponseList stageExecutionResponseList = new StageExecutionResponseList();
    stageExecutionResponseList.addAll(stagesExecutions);
    return Response.ok().entity(stageExecutionResponseList).build();
  }

  private List<StageExecutionResponseBody> getStageExecutionResponse(
      List<StageExecutionResponse> stageExecutionResponse) {
    List<StageExecutionResponseBody> stageExecResponse = new ArrayList<>();
    for (StageExecutionResponse stageRes : stageExecutionResponse) {
      StageExecutionResponseBody response = new StageExecutionResponseBody();
      response.setMessage(stageRes.getMessage());
      response.setStageIdentifier(stageRes.getStageIdentifier());
      response.setStageName(stageRes.getStageName());
      response.setStagesRequired(stageRes.getStagesRequired());
      response.setIsToBeBlocked(stageRes.isToBeBlocked());
      stageExecResponse.add(response);
    }
    return stageExecResponse;
  }

  @Override
  public Response rerunPipeline(String org, String project, String pipeline, String executionId,
      @Valid RerunPipelineRequestBody body, String harnessAccount, String module, Boolean useFqnIfError, String notes,
      String branchName, String connectorRef, String repoName) {
    GitAwareContextHelper.populateGitDetails(
        GitEntityInfo.builder().branch(branchName).connectorRef(connectorRef).repoName(repoName).build());
    if (pmsFeatureFlagService.isEnabled(harnessAccount, PIE_GET_FILE_CONTENT_ONLY)) {
      ThreadOperationContextHelper.setUserFlow(USER_FLOW.EXECUTION);
    }
    String inputSetPipelineYaml = null;
    if (body != null) {
      inputSetPipelineYaml = body.getInputsYaml();
    }
    PlanExecutionResponseDto planExecutionResponseDto = pipelineExecutor.runPipelineWithInputSetPipelineYaml(
        harnessAccount, org, project, pipeline, module, inputSetPipelineYaml, false, false, notes);
    PipelineExecuteResponseBody pipelineExecuteResponseBody =
        getPipelineExecutionResponseFromPlanExecutionResponse(planExecutionResponseDto);
    return Response.ok().entity(pipelineExecuteResponseBody).build();
  }

  @Override
  public Response rerunStagesExecutionOfPipeline(String org, String project, String pipeline, String executionId,
      @Valid RunStageRequestBody body, String harnessAccount, String branchName, String connectorRef, String repoName,
      Boolean useFqnIfError, String module, String notes) {
    if (pmsFeatureFlagService.isEnabled(harnessAccount, PIE_GET_FILE_CONTENT_ONLY)) {
      ThreadOperationContextHelper.setUserFlow(USER_FLOW.EXECUTION);
    }
    RunStageRequestDTO runStageRequestDTO = getRunStageRequestDTO(body);
    PlanExecutionResponseDto planExecutionResponseDto = pipelineExecutor.rerunStagesWithRuntimeInputYaml(
        harnessAccount, org, project, pipeline, module, executionId, runStageRequestDTO, false, false, notes);
    PipelineExecuteResponseBody pipelineExecuteResponseBody =
        getPipelineExecutionResponseFromPlanExecutionResponse(planExecutionResponseDto);
    return Response.ok().entity(pipelineExecuteResponseBody).build();
  }

  @Override
  public Response retryPipelineWithInputsetPipelineYaml(String org, String project, String pipeline, String executionId,
      @Valid RerunPipelineRequestBody body, String harnessAccount, String module, List retryStages,
      Boolean runAllStages, String notes) {
    if (pmsFeatureFlagService.isEnabled(harnessAccount, PIE_GET_FILE_CONTENT_ONLY)) {
      ThreadOperationContextHelper.setUserFlow(USER_FLOW.EXECUTION);
    }
    if (retryStages.size() == 0) {
      throw new InvalidRequestException("You need to select the stage to retry!!");
    }

    RetryInfo retryInfo = retryExecutionHelper.validateRetry(harnessAccount, org, project, pipeline, executionId, null);
    if (!retryInfo.isResumable()) {
      throw new InvalidRequestException(retryInfo.getErrorMessage());
    }
    String inputSetPipelineYaml = null;
    if (body != null) {
      inputSetPipelineYaml = body.getInputsYaml();
    }
    PlanExecutionResponseDto planExecutionResponseDto =
        pipelineExecutor.retryPipelineWithInputSetPipelineYaml(harnessAccount, org, project, pipeline, module,
            inputSetPipelineYaml, executionId, retryStages, runAllStages, false, false, notes);
    PipelineExecuteResponseBody pipelineExecuteResponseBody =
        getPipelineExecutionResponseFromPlanExecutionResponse(planExecutionResponseDto);
    return Response.ok().entity(pipelineExecuteResponseBody).build();
  }

  PipelineExecuteResponseBody getPipelineExecutionResponseFromPlanExecutionResponse(
      PlanExecutionResponseDto planExecutionResponseDto) {
    PipelineExecuteResponseBody pipelineExecuteResponseBody = new PipelineExecuteResponseBody();
    ExecutionDetails executionDetails = new ExecutionDetails();
    executionDetails.setExecutionId(planExecutionResponseDto.getPlanExecution().getUuid());
    executionDetails.setStatus(planExecutionResponseDto.getPlanExecution().getStatus().toString());
    pipelineExecuteResponseBody.setExecutionDetails(executionDetails);
    return pipelineExecuteResponseBody;
  }
}
