/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.plancreator.approval.v1;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidYamlException;
import io.harness.plancreator.stages.v1.AbstractStagePlanCreator;
import io.harness.plancreator.stages.v1.StageParameterUtilsV1;
import io.harness.plancreator.steps.common.v1.StageElementParametersV1.StageElementParametersV1Builder;
import io.harness.plancreator.strategy.StrategyUtilsV1;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.PlanNode.PlanNodeBuilder;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.steps.approval.stage.ApprovalStageSpecParameters;
import io.harness.steps.approval.stage.v1.ApprovalStageNodeV1;
import io.harness.steps.approval.stage.v1.ApprovalStageStep;
import io.harness.when.utils.v1.RunInfoUtilsV1;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public class ApprovalStagePlanCreator extends AbstractStagePlanCreator<ApprovalStageNodeV1> {
  @Override
  public ApprovalStageNodeV1 getFieldObject(YamlField field) {
    try {
      return YamlUtils.read(field.getNode().toString(), ApprovalStageNodeV1.class);
    } catch (IOException e) {
      throw new InvalidYamlException(
          "Unable to parse approval stage yaml. Please ensure that it is in correct format", e);
    }
  }
  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, ApprovalStageNodeV1 field) {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();
    Map<String, YamlField> dependenciesNodeMap = new HashMap<>();

    YamlField specField =
        Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.SPEC));

    // Add dependency for execution
    YamlField stepsField = specField.getNode().getField(YAMLFieldNameConstants.STEPS);
    if (stepsField == null) {
      throw new InvalidRequestException("Steps section is required in Approval stage");
    }
    dependenciesNodeMap.put(specField.getNode().getUuid(), specField);

    // adding support for strategy
    Dependency strategyDependency = getDependencyForStrategy(dependenciesNodeMap, field, ctx);

    planCreationResponseMap.put(specField.getNode().getUuid(),
        PlanCreationResponse.builder()
            .dependencies(DependenciesUtils.toDependenciesProto(dependenciesNodeMap)
                              .toBuilder()
                              .putDependencyMetadata(field.getUuid(), strategyDependency)
                              .putDependencyMetadata(specField.getNode().getUuid(), getDependencyForChildren(field))
                              .build())
            .build());

    return planCreationResponseMap;
  }
  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, ApprovalStageNodeV1 field, List<String> childrenNodeIds) {
    StageElementParametersV1Builder stageParameters = StageParameterUtilsV1.getCommonStageParameters(field);
    stageParameters.type(YAMLFieldNameConstants.APPROVAL_V1);
    stageParameters.spec(ApprovalStageSpecParameters.builder().childNodeID(childrenNodeIds.get(0)).build());
    String name = field.getName();
    PlanNodeBuilder builder =
        PlanNode.builder()
            .uuid(StrategyUtilsV1.getSwappedPlanNodeId(ctx, field.getUuid()))
            .identifier(StrategyUtilsV1.getIdentifierWithExpression(ctx, field.getId()))
            .stepType(ApprovalStageStep.STEP_TYPE)
            .group(StepOutcomeGroup.STAGE.name())
            .name(StrategyUtilsV1.getIdentifierWithExpression(ctx, name))
            .skipUnresolvedExpressionsCheck(true)
            .whenCondition(RunInfoUtilsV1.getStageWhenCondition(field.getWhen()))
            .stepParameters(stageParameters.build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                    .build())
            .exports(field.getExports())
            .skipExpressionChain(false);

    // If strategy present then don't add advisers. Strategy node will take care of running the stage nodes.
    if (field.getStrategy() == null) {
      builder.adviserObtainments(getAdviserObtainments(ctx.getDependency()));
    }
    return builder.build();
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(
        YAMLFieldNameConstants.STAGE, Collections.singleton(YAMLFieldNameConstants.APPROVAL_V1));
  }
}
