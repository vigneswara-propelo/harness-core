/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.execution.plan.creator.stage.V3;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.stages.IntegrationStageStepParametersPMS;
import io.harness.beans.steps.StepSpecTypeConstants;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.ci.execution.integrationstage.IntegrationStageUtils;
import io.harness.ci.execution.integrationstage.V1.CIPlanCreatorUtils;
import io.harness.ci.execution.plan.creator.codebase.CodebasePlanCreator;
import io.harness.ci.execution.states.IntegrationStageStepPMS;
import io.harness.cimanager.stages.V1.IntegrationStageConfigImplV1;
import io.harness.cimanager.stages.V1.IntegrationStageNodeV1;
import io.harness.exception.InvalidYamlException;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.stages.v1.AbstractStagePlanCreator;
import io.harness.plancreator.steps.common.StageElementParameters;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.HarnessStruct;
import io.harness.pms.contracts.plan.HarnessValue;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.yaml.clone.Clone;
import io.harness.yaml.extended.ci.codebase.CodeBase;
import io.harness.yaml.options.Options;
import io.harness.yaml.registry.Registry;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class IntegrationStagePMSPlanCreatorV3 extends AbstractStagePlanCreator<IntegrationStageNodeV1> {
  @Inject private KryoSerializer kryoSerializer;
  @Inject private CIPlanCreatorUtils ciPlanCreatorUtils;
  public static final String STAGE_NODE = "stageNode";
  public static final String INFRASTRUCTURE = "infrastructure";
  public static final String CODEBASE = "codebase";

  @Override
  public IntegrationStageNodeV1 getFieldObject(YamlField field) {
    try {
      return YamlUtils.read(field.getNode().toString(), IntegrationStageNodeV1.class);
    } catch (IOException e) {
      throw new InvalidYamlException(
          "Unable to parse integration stage yaml. Please ensure that it is in correct format", e);
    }
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(
        YAMLFieldNameConstants.STAGE, Collections.singleton(StepSpecTypeConstants.CI_STAGE_V2));
  }

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, IntegrationStageNodeV1 stageNode) {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();
    Map<String, YamlField> dependenciesNodeMap = new HashMap<>();
    YamlField field = ctx.getCurrentField();
    YamlField specField = Preconditions.checkNotNull(field.getNode().getField(YAMLFieldNameConstants.SPEC));
    IntegrationStageConfigImplV1 stageConfigImpl = stageNode.getStageConfig();
    Infrastructure infrastructure =
        ciPlanCreatorUtils.getInfrastructure(stageConfigImpl.getRuntime(), stageConfigImpl.getPlatform());
    CodeBase codeBase =
        createPlanForCodebase(ctx, stageConfigImpl.getClone(), planCreationResponseMap, specField.getUuid());
    dependenciesNodeMap.put(specField.getUuid(), specField);

    planCreationResponseMap.put(specField.getUuid(),
        PlanCreationResponse.builder()
            .dependencies(DependenciesUtils.toDependenciesProto(dependenciesNodeMap)
                              .toBuilder()
                              .putDependencyMetadata(
                                  field.getUuid(), getDependencyForStrategy(dependenciesNodeMap, stageNode, ctx))
                              .putDependencyMetadata(specField.getUuid(),
                                  getDependencyMetadataForStepsField(infrastructure, codeBase, stageNode))
                              .build())
            .build());
    log.info("Successfully created plan for integration stage {}", stageNode.getName());
    return planCreationResponseMap;
  }

  private CodeBase createPlanForCodebase(PlanCreationContext ctx, Clone stageClone,
      LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap, String childNodeID) {
    Optional<CodeBase> optionalCodeBase = ciPlanCreatorUtils.getCodebase(ctx, stageClone);
    if (optionalCodeBase.isPresent()) {
      CodeBase codeBase = optionalCodeBase.get();
      List<PlanNode> codebasePlanNodes =
          CodebasePlanCreator.buildCodebasePlanNodes(generateUuid(), childNodeID, kryoSerializer, codeBase, null);
      if (isNotEmpty(codebasePlanNodes)) {
        Collections.reverse(codebasePlanNodes);
        for (PlanNode planNode : codebasePlanNodes) {
          planCreationResponseMap.put(planNode.getUuid(), PlanCreationResponse.builder().planNode(planNode).build());
        }
      }
      return codeBase;
    }
    return null;
  }

  Dependency getDependencyMetadataForStepsField(
      Infrastructure infrastructure, CodeBase codeBase, IntegrationStageNodeV1 stageNode) {
    Map<String, HarnessValue> nodeMetadataMap = new HashMap<>();
    Map<String, ByteString> metadataMap = new HashMap<>();
    ByteString stageNodeBytes = ByteString.copyFrom(kryoSerializer.asBytes(stageNode));
    ByteString infrastructureBytes = ByteString.copyFrom(kryoSerializer.asBytes(infrastructure));
    metadataMap.put(STAGE_NODE, stageNodeBytes);
    metadataMap.put(INFRASTRUCTURE, infrastructureBytes);

    nodeMetadataMap.put(STAGE_NODE, HarnessValue.newBuilder().setBytesValue(stageNodeBytes).build());
    nodeMetadataMap.put(INFRASTRUCTURE, HarnessValue.newBuilder().setBytesValue(infrastructureBytes).build());
    if (codeBase != null) {
      ByteString codebaseBytes = ByteString.copyFrom(kryoSerializer.asBytes(codeBase));
      metadataMap.put(CODEBASE, codebaseBytes);
      nodeMetadataMap.put(CODEBASE, HarnessValue.newBuilder().setBytesValue(codebaseBytes).build());
    }
    return Dependency.newBuilder()
        .setNodeMetadata(HarnessStruct.newBuilder().putAllData(nodeMetadataMap).build())
        .build();
  }

  @Override
  public StepType getStepType() {
    return IntegrationStageStepPMS.STEP_TYPE;
  }

  @Override
  public StepParameters getStageParameters(
      PlanCreationContext ctx, IntegrationStageNodeV1 stageNodeV1, List<String> childrenNodeIds) {
    YamlField field = ctx.getCurrentField();
    YamlField specField = Preconditions.checkNotNull(field.getNode().getField(YAMLFieldNameConstants.SPEC));
    YamlField stepsField = Preconditions.checkNotNull(specField.getNode().getField(YAMLFieldNameConstants.STEPS));
    List<YamlField> steps = CIPlanCreatorUtils.getStepYamlFields(stepsField);
    Optional<Options> optionalOptions =
        ciPlanCreatorUtils.getDeserializedOptions(ctx.getMetadata().getGlobalDependency());
    Options options = optionalOptions.orElse(Options.builder().build());

    IntegrationStageConfigImplV1 stageConfig = stageNodeV1.getStageConfig();
    Infrastructure infrastructure =
        ciPlanCreatorUtils.getInfrastructure(stageConfig.getRuntime(), stageConfig.getPlatform());
    CodeBase codeBase = ciPlanCreatorUtils.getCodebase(ctx, stageConfig.getClone()).orElse(null);
    Registry registry = options.getRegistry() == null ? Registry.builder().build() : options.getRegistry();
    List<ExecutionWrapperConfig> executionWrapperConfigs =
        steps.stream().map(CIPlanCreatorUtils::getExecutionConfig).collect(Collectors.toList());
    IntegrationStageStepParametersPMS params =
        IntegrationStageStepParametersPMS.builder()
            .stepIdentifiers(IntegrationStageUtils.getStepIdentifiers(executionWrapperConfigs))
            .infrastructure(infrastructure)
            .childNodeID(childrenNodeIds.get(0))
            .codeBase(codeBase)
            .triggerPayload(ctx.getTriggerPayload())
            .registry(registry)
            .cloneManually(ciPlanCreatorUtils.shouldCloneManually(ctx, codeBase))
            .build();
    return StageElementParameters.builder()
        .identifier(stageNodeV1.getId())
        .name(stageNodeV1.getName())
        .specConfig(params)
        .build();
  }
}