/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.approval.notification.stagemetadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.util.Objects.isNull;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.Scope;
import io.harness.cd.CDStageSummaryConstants;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.cdstage.CDStageSummaryResponseDTO;
import io.harness.pms.approval.notification.ApprovalSummary;
import io.harness.pms.plan.execution.beans.dto.GraphLayoutNodeDTO;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_APPROVALS})
@OwnedBy(CDC)
public interface StageMetadataNotificationHelper {
  String STAGE_NODE_TYPE = "STAGE";
  String DEPLOYMENT_STAGE_TYPE = "Deployment";
  String CD_STAGE_METADATA_ROW_FORMAT = "     %s  :  %s";
  String CD_STAGE_HEADER_FORMAT = "%s : ";

  /**
   * if finishedStages set is ordered, then guarantees same insertion order in formattedFinishedStages
   * */
  void setFormattedSummaryOfFinishedStages(
      @NotNull Set<StageSummary> finishedStages, @NotNull Set<String> formattedFinishedStages, @NotNull Scope scope);

  /**
   * if runningStages set is ordered, then guarantees same insertion order in formattedRunningStages
   * */
  void setFormattedSummaryOfRunningStages(@NotNull Set<StageSummary> runningStages,
      @NotNull Set<String> formattedRunningStages, @NotNull Scope scope, @NotNull String planExecutionId);

  /**
   * if upcomingStages set is ordered, then guarantees same insertion order in formattedUpcomingStages
   * */
  void setFormattedSummaryOfUpcomingStages(@NotNull Set<StageSummary> upcomingStages,
      @NotNull Set<String> formattedUpcomingStages, @NotNull Scope scope, @NotNull String planExecutionId);

  static boolean isGraphNodeOfCDDeploymentStageType(@NotNull GraphLayoutNodeDTO node) {
    // this check is enough to check CD deployment stages, as another module won't have stages with same type
    // "Deployment" we are not checking module info as in some cases module info is not being populated correctly ex1:
    // running looping CD stages, multiService cd stages, etc.
    return STAGE_NODE_TYPE.equals(node.getNodeGroup()) && DEPLOYMENT_STAGE_TYPE.equals(node.getNodeType());
  }

  /**
   * add details of stage node to a stages set depending on stage type
   *
   * @param stages ongoing set containing stages summary ; can be either already executed, running or upcoming stages
   * @param node stage node
   *
   */
  static void addStageNodeToStagesSummary(@NotNull Set<StageSummary> stages, @NotNull GraphLayoutNodeDTO node) {
    if (isNull(node) || isNull(stages)) {
      throw new InvalidRequestException(
          "Input stage node and stages set is required for adding details of stage node to a stages set");
    }
    if (isGraphNodeOfCDDeploymentStageType(node)) {
      CDStageSummary cdStageSummary = CDStageSummary.builder().build();
      cdStageSummary.setStageIdentifier(node.getNodeIdentifier());
      cdStageSummary.setStageExecutionIdentifier(node.getNodeExecutionId());
      cdStageSummary.setStageName(node.getName());
      stages.add(cdStageSummary);
    } else {
      GenericStageSummary genericStageSummary = GenericStageSummary.builder().build();
      genericStageSummary.setStageIdentifier(node.getNodeIdentifier());
      genericStageSummary.setStageExecutionIdentifier(node.getNodeExecutionId());
      genericStageSummary.setStageName(node.getName());
      stages.add(genericStageSummary);
    }
  }

  /**
   *
   * example format for single service environment:
   * stage Name :
   *      Service  :  service Name
   *      Environment  :  env Name
   *      Infrastructure Definition  :  infra Name
   */
  static String formatCDStageMetadata(
      @Nullable CDStageSummaryResponseDTO cdStageSummaryResponseDTO, @NotNull CDStageSummary cdStageSummary) {
    if (isNull(cdStageSummary)) {
      throw new InvalidRequestException("CD stage details required for getting formatted stage summary");
    }
    if (isNull(cdStageSummaryResponseDTO)) {
      return cdStageSummary.getFormattedEntityName();
    }

    Set<String> rows = new HashSet<>();

    if (StringUtils.isNotBlank(cdStageSummaryResponseDTO.getService())) {
      rows.add(String.format(
          CD_STAGE_METADATA_ROW_FORMAT, CDStageSummaryConstants.SERVICE, cdStageSummaryResponseDTO.getService()));
    }
    if (StringUtils.isNotBlank(cdStageSummaryResponseDTO.getEnvironment())) {
      rows.add(String.format(CD_STAGE_METADATA_ROW_FORMAT, CDStageSummaryConstants.ENVIRONMENT,
          cdStageSummaryResponseDTO.getEnvironment()));
    }
    if (StringUtils.isNotBlank(cdStageSummaryResponseDTO.getInfra())) {
      rows.add(String.format(CD_STAGE_METADATA_ROW_FORMAT, CDStageSummaryConstants.INFRA_DEFINITION,
          cdStageSummaryResponseDTO.getInfra()));
    }

    if (rows.isEmpty()) {
      return cdStageSummary.getFormattedEntityName();
    }
    String formattedMetadata = String.join("\n", rows);
    String formattedHeader = String.format(CD_STAGE_HEADER_FORMAT, cdStageSummary.getFormattedEntityName());
    return String.join("\n", formattedHeader, formattedMetadata);
  }

  /**
   * assumes approvalSummary stages sets are instantiated with ordered set.
   * assuming input sets in stagesSummary are ordered,
   * then guarantees to maintain the input order in formatted stage metadata
   */
  static void addStageMetadataWhenFFOff(
      @NotNull StagesSummary stagesSummary, @NotNull ApprovalSummary approvalSummary) {
    if (isNull(stagesSummary) || isNull(approvalSummary)) {
      throw new InvalidRequestException(
          "Stages details and approval summary are required for setting approval formatted stage summary");
    }
    approvalSummary.getUpcomingStages().addAll(stagesSummary.getUpcomingStages()
                                                   .stream()
                                                   .map(StageSummary::getFormattedEntityName)
                                                   .collect(Collectors.toCollection(LinkedHashSet::new)));
    approvalSummary.getFinishedStages().addAll(stagesSummary.getFinishedStages()
                                                   .stream()
                                                   .map(StageSummary::getFormattedEntityName)
                                                   .collect(Collectors.toCollection(LinkedHashSet::new)));
    approvalSummary.getRunningStages().addAll(stagesSummary.getRunningStages()
                                                  .stream()
                                                  .map(StageSummary::getFormattedEntityName)
                                                  .collect(Collectors.toCollection(LinkedHashSet::new)));
  }
}
