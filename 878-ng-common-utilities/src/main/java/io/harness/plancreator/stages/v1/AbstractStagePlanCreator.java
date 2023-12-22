/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.plancreator.stages.v1;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.plancreator.PlanCreatorUtilsV1;
import io.harness.plancreator.stages.stage.v1.AbstractStageNodeV1;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.strategy.StrategyUtilsV1;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.contracts.plan.HarnessStruct;
import io.harness.pms.contracts.plan.HarnessValue;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.plan.creation.PlanCreatorConstants;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.PlanNode.PlanNodeBuilder;
import io.harness.pms.sdk.core.plan.creation.beans.GraphLayoutResponse;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.timeout.SdkTimeoutObtainment;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlField;
import io.harness.serializer.KryoSerializer;
import io.harness.when.utils.v1.RunInfoUtilsV1;
import io.harness.yaml.core.failurestrategy.v1.FailureConfigV1;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(HarnessTeam.PIPELINE)
public abstract class AbstractStagePlanCreator<T extends AbstractStageNodeV1> extends ChildrenPlanCreator<T> {
  @Inject public KryoSerializer kryoSerializer;

  public Dependency getDependencyForStrategy(
      Map<String, YamlField> dependenciesNodeMap, T stageNode, PlanCreationContext ctx) {
    Map<String, HarnessValue> dependencyMetadata = StrategyUtilsV1.getStrategyFieldDependencyMetadataIfPresent(
        kryoSerializer, ctx, stageNode.getUuid(), dependenciesNodeMap, getAdviserObtainments(ctx.getDependency()));
    return Dependency.newBuilder()
        .setNodeMetadata(HarnessStruct.newBuilder().putAllData(dependencyMetadata).build())
        .build();
  }

  public Dependency getDependencyForChildren(T stageNode) {
    Map<String, HarnessValue> data = new HashMap<>();
    ParameterField<List<TaskSelectorYaml>> delegates = stageNode.getDelegates();
    if (ParameterField.isNotNull(delegates)) {
      data.put(PlanCreatorConstants.STAGE_DELEGATES,
          HarnessValue.newBuilder()
              .setBytesValue(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(delegates)))
              .build());
    }
    List<FailureConfigV1> stageFailureStrategies = null;
    if (isNotEmpty(stageFailureStrategies)) {
      data.put(PlanCreatorConstants.STAGE_FAILURE_STRATEGIES,
          HarnessValue.newBuilder()
              .setBytesValue(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(stageFailureStrategies)))
              .build());
    }
    return Dependency.newBuilder().setParentInfo(HarnessStruct.newBuilder().putAllData(data).build()).build();
  }

  public List<AdviserObtainment> getAdviserObtainments(Dependency dependency) {
    return PlanCreatorUtilsV1.getAdviserObtainmentsForStage(kryoSerializer, dependency);
  }

  public GraphLayoutResponse getLayoutNodeInfo(PlanCreationContext context, T stageNode) {
    Map<String, GraphLayoutNode> stageYamlFieldMap = new LinkedHashMap<>();
    YamlField stageYamlField = context.getCurrentField();
    String nextNodeUuid = PlanCreatorUtilsV1.getNextNodeUuid(kryoSerializer, context.getDependency());
    if (StrategyUtilsV1.isWrappedUnderStrategy(context.getCurrentField())) {
      stageYamlFieldMap = StrategyUtilsV1.modifyStageLayoutNodeGraph(stageYamlField, nextNodeUuid);
    }
    return GraphLayoutResponse.builder().layoutNodes(stageYamlFieldMap).build();
  }

  @Override
  public Set<String> getSupportedYamlVersions() {
    return Set.of(HarnessYamlVersion.V1);
  }

  @Override
  public PlanNode createPlanForParentNode(PlanCreationContext ctx, T stageNodeV1, List<String> childrenNodeIds) {
    StepParameters stageParameters = getStageParameters(ctx, stageNodeV1, childrenNodeIds);
    SdkTimeoutObtainment timeoutObtainment = PlanCreatorUtilsV1.getTimeoutObtainmentForStage(stageNodeV1);
    PlanNodeBuilder builder =
        PlanNode.builder()
            .uuid(StrategyUtilsV1.getSwappedPlanNodeId(ctx, stageNodeV1.getUuid()))
            .identifier(StrategyUtilsV1.getIdentifierWithExpression(ctx, stageNodeV1.getId()))
            .stepType(getStepType())
            .group(StepOutcomeGroup.STAGE.name())
            .name(StrategyUtilsV1.getIdentifierWithExpression(ctx, stageNodeV1.getName()))
            .skipUnresolvedExpressionsCheck(true)
            .whenCondition(RunInfoUtilsV1.getStageWhenCondition(stageNodeV1.getWhen()))
            .stepParameters(stageParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                    .build())
            .exports(stageNodeV1.getExports())
            .adviserObtainments(getAdviserObtainments(ctx, stageNodeV1))
            .skipExpressionChain(false);

    if (timeoutObtainment != null) {
      builder.timeoutObtainment(timeoutObtainment);
    }
    // If strategy present then don't add advisers. Strategy node will take care of running the stage nodes.
    if (stageNodeV1.getStrategy() == null) {
      builder.adviserObtainments(getAdviserObtainments(ctx.getDependency()));
    }

    if (!EmptyPredicate.isEmpty(ctx.getExecutionInputTemplate())) {
      builder.executionInputTemplate(ctx.getExecutionInputTemplate());
    }
    return builder.build();
  }

  public abstract StepParameters getStageParameters(
      PlanCreationContext ctx, T stageNodeV1, List<String> childrenNodeIds);

  public abstract StepType getStepType();

  public List<AdviserObtainment> getAdviserObtainments(PlanCreationContext ctx, T node) {
    return new ArrayList<>();
  }
}