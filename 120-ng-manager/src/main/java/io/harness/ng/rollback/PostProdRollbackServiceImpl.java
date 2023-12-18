/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.rollback;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.dtos.rollback.PostProdRollbackCheckDTO;
import io.harness.dtos.rollback.PostProdRollbackResponseDTO;
import io.harness.dtos.rollback.PostProdRollbackSwimLaneInfo;
import io.harness.entities.Instance;
import io.harness.entities.InstanceType;
import io.harness.entities.RollbackStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.pipeline.remote.PipelineServiceClient;
import io.harness.pms.contracts.execution.Status;
import io.harness.remote.client.NGRestUtils;
import io.harness.repositories.instance.InstanceRepository;

import com.google.inject.Inject;
import java.util.Map;
import java.util.Set;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_ECS})
@OwnedBy(HarnessTeam.CDP)
public class PostProdRollbackServiceImpl implements PostProdRollbackService {
  // Set of instanceTypes that supports the PostProdRollback.
  private static final Set<InstanceType> SUPPORTED_INSTANCE_TYPES_FOR_ROLLBACK =
      Set.of(InstanceType.K8S_INSTANCE, InstanceType.TAS_INSTANCE, InstanceType.ECS_INSTANCE, InstanceType.ASG_INSTANCE,
          InstanceType.SPOT_INSTANCE, InstanceType.NATIVE_HELM_INSTANCE);
  private static final Set<RollbackStatus> ALLOWED_ROLLBACK_START_STATUSES =
      Set.of(RollbackStatus.NOT_STARTED, RollbackStatus.UNAVAILABLE);
  @Inject private PipelineServiceClient pipelineServiceClient;
  @Inject private InstanceRepository instanceRepository;
  @Inject private PostProdRollbackHelperUtils postProdRollbackHelperUtils;
  @Override
  public PostProdRollbackCheckDTO checkIfRollbackAllowed(
      String accountIdentifier, String instanceKey, String infraMappingId) {
    boolean isRollbackAllowed = true;
    String message = null;
    Instance instance =
        instanceRepository.getInstanceByInstanceKeyAndInfrastructureMappingId(instanceKey, infraMappingId);
    if (instance == null) {
      throw new InvalidRequestException(String.format(
          "Could not find the instance for InstanceKey %s and infraMappingId %s", instanceKey, infraMappingId));
    }
    if (instance.getStageStatus() != Status.SUCCEEDED) {
      isRollbackAllowed = false;
      message = String.format(
          "The deployment stage was not successful in latest execution %s", instance.getLastPipelineExecutionId());
    } else if (!SUPPORTED_INSTANCE_TYPES_FOR_ROLLBACK.contains(instance.getInstanceType())) {
      isRollbackAllowed = false;
      message =
          String.format("The given instanceType %s is not supported for rollback.", instance.getInstanceType().name());
    }
    if (instance.getRollbackStatus() == null) {
      isRollbackAllowed = false;
      message = "Unable to determine rollback status for given Instance";
    } else if (!ALLOWED_ROLLBACK_START_STATUSES.contains(instance.getRollbackStatus())) {
      isRollbackAllowed = false;
      message = String.format(
          "Can not start the Rollback. Rollback has already been triggered and the previous rollback status is: %s",
          instance.getRollbackStatus());
    }

    PostProdRollbackSwimLaneInfo swimLaneInfo = null;
    if (isRollbackAllowed) {
      swimLaneInfo = postProdRollbackHelperUtils.getSwimlaneInfo(instance);
    }
    return PostProdRollbackCheckDTO.builder()
        .isRollbackAllowed(isRollbackAllowed)
        .message(message)
        .swimLaneInfo(swimLaneInfo)
        .build();
  }

  @Override
  public PostProdRollbackResponseDTO triggerRollback(
      String accountIdentifier, String instanceKey, String infraMappingId) {
    PostProdRollbackCheckDTO checkDTO = checkIfRollbackAllowed(accountIdentifier, instanceKey, infraMappingId);
    if (!checkDTO.isRollbackAllowed()) {
      return PostProdRollbackResponseDTO.builder()
          .isRollbackTriggered(false)
          .instanceKey(instanceKey)
          .infraMappingId(infraMappingId)
          .message(checkDTO.getMessage())
          .build();
    }
    Instance instance =
        instanceRepository.getInstanceByInstanceKeyAndInfrastructureMappingId(instanceKey, infraMappingId);
    Object response = null;
    try {
      // TODO: Get the pipelineIdentifier. That would be used for doing the RBAC check.
      response = NGRestUtils.getResponse(pipelineServiceClient.triggerPostExecutionRollback(
          instance.getLastPipelineExecutionId(), instance.getAccountIdentifier(), instance.getOrgIdentifier(),
          instance.getProjectIdentifier(), "getPipelineId", instance.getStageNodeExecutionId()));
    } catch (Exception ex) {
      throw new InvalidRequestException(
          String.format("Could not trigger the rollback for instance with InstanceKey %s and infraMappingId %s: %s",
              instanceKey, infraMappingId, ex.getMessage()),
          ex);
    }
    String planExecutionId = (String) (((Map<String, Map>) response).get("planExecution")).get("uuid");
    // since rollback execution is triggered then mark the rollbackStatus as STARTED.
    instance.setRollbackStatus(RollbackStatus.STARTED);
    instanceRepository.save(instance);
    return PostProdRollbackResponseDTO.builder()
        .isRollbackTriggered(true)
        .instanceKey(instanceKey)
        .infraMappingId(infraMappingId)
        .planExecutionId(planExecutionId)
        .build();
  }
}
