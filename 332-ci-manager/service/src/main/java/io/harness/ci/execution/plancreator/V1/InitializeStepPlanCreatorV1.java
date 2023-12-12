/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.plancreator.V1;

import static io.harness.beans.FeatureName.QUEUE_CI_EXECUTIONS_CONCURRENCY;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.nodes.InitializeStepNode;
import io.harness.beans.steps.stepinfo.InitializeStepInfo;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.beans.yaml.extended.infrastrucutre.VmInfraYaml;
import io.harness.beans.yaml.extended.infrastrucutre.VmPoolYaml;
import io.harness.ci.execution.integrationstage.BuildJobEnvInfoBuilder;
import io.harness.ci.execution.integrationstage.VmInitializeTaskParamsBuilder;
import io.harness.ci.ff.CIFeatureFlagService;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreatorV2;
import io.harness.cimanager.stages.IntegrationStageConfigImpl;
import io.harness.exception.InvalidYamlException;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.stages.stage.v1.AbstractStageNodeV1;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.utils.IdentifierGeneratorUtils;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.yaml.core.timeout.Timeout;
import io.harness.yaml.extended.ci.codebase.CodeBase;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@OwnedBy(HarnessTeam.CI)
public class InitializeStepPlanCreatorV1 extends CIPMSStepPlanCreatorV2<InitializeStepNode> {
  private final String InitializeDisplayName = "Initialize";
  @Inject private BuildJobEnvInfoBuilder buildJobEnvInfoBuilder;
  @Inject private CIFeatureFlagService ffService;

  public PlanCreationResponse createPlan(PlanCreationContext ctx, AbstractStageNodeV1 abstractStageNode,
      CodeBase codebase, Infrastructure infrastructure, List<ExecutionWrapperConfig> executionWrapperConfigs,
      String childID) {
    // create PluginStepNode
    InitializeStepNode initializeStepNode =
        getStepNode(ctx, codebase, infrastructure, abstractStageNode, executionWrapperConfigs);
    // create Plan node
    return createInternalStepPlan(ctx, initializeStepNode, childID);
  }

  private InitializeStepNode getStepNode(PlanCreationContext ctx, CodeBase codeBase, Infrastructure infrastructure,
      AbstractStageNodeV1 abstractStageNode, List<ExecutionWrapperConfig> executionWrapperConfigs) {
    // TODO: create InitializeStepInfoV1
    InitializeStepInfo initializeStepInfo =
        InitializeStepInfo.builder()
            .identifier(InitializeStepInfo.STEP_TYPE.getType())
            .name(InitializeStepInfo.STEP_TYPE.getType())
            .infrastructure(infrastructure)
            .stageIdentifier(abstractStageNode.getId())
            // TODO: set variables once InitializeStepInfoV1 is created
            //            .variables(abstractStageNode.getVariables())
            .stageElementConfig(IntegrationStageConfigImpl.builder()
                                    .uuid(IdentifierGeneratorUtils.getId(abstractStageNode.getName()))
                                    .execution(ExecutionElementConfig.builder().steps(executionWrapperConfigs).build())
                                    .infrastructure(infrastructure)
                                    .cloneCodebase(ParameterField.createValueField(codeBase != null))
                                    .serviceDependencies(ParameterField.createValueField(Collections.emptyList()))
                                    .build())
            .ciCodebase(codeBase)
            .skipGitClone(codeBase == null)
            .executionElementConfig(ExecutionElementConfig.builder().steps(executionWrapperConfigs).build())
            .timeout(buildJobEnvInfoBuilder.getTimeout(infrastructure, ctx.getAccountIdentifier()))
            .build();

    return InitializeStepNode.builder()
        .identifier(InitializeStepInfo.STEP_TYPE.getType())
        .name(InitializeDisplayName)
        .uuid(generateUuid())
        .type(InitializeStepNode.StepType.liteEngineTask)
        .timeout(getTimeout(infrastructure, ctx.getAccountIdentifier()))
        .initializeStepInfo(initializeStepInfo)
        .build();
  }

  private ParameterField<Timeout> getTimeout(Infrastructure infrastructure, String accountId) {
    if (infrastructure == null) {
      throw new CIStageExecutionException("Input infrastructure can not be empty");
    }
    boolean queueEnabled = ffService.isEnabled(QUEUE_CI_EXECUTIONS_CONCURRENCY, accountId);
    if (queueEnabled) {
      return ParameterField.createValueField(Timeout.fromString("10h"));
    }
    if (infrastructure.getType() == Infrastructure.Type.VM) {
      VmInitializeTaskParamsBuilder.validateInfrastructure(infrastructure);
      VmPoolYaml vmPoolYaml = (VmPoolYaml) ((VmInfraYaml) infrastructure).getSpec();
      return parseTimeout(vmPoolYaml.getSpec().getInitTimeout(), "15m");
    } else if (infrastructure.getType() == Infrastructure.Type.KUBERNETES_DIRECT) {
      if (((K8sDirectInfraYaml) infrastructure).getSpec() == null) {
        throw new CIStageExecutionException("Input infrastructure can not be empty");
      }
      ParameterField<String> timeout = ((K8sDirectInfraYaml) infrastructure).getSpec().getInitTimeout();
      return parseTimeout(timeout, "10m");
    }
    return ParameterField.createValueField(Timeout.fromString("10m"));
  }

  private ParameterField<Timeout> parseTimeout(ParameterField<String> timeout, String defaultTimeout) {
    if (timeout != null && timeout.fetchFinalValue() != null && isNotEmpty((String) timeout.fetchFinalValue())) {
      return ParameterField.createValueField(Timeout.fromString((String) timeout.fetchFinalValue()));
    } else {
      return ParameterField.createValueField(Timeout.fromString(defaultTimeout));
    }
  }

  @Override
  public Set<String> getSupportedStepTypes() {
    return null;
  }

  @Override
  public InitializeStepNode getFieldObject(YamlField field) {
    try {
      return YamlUtils.read(field.getNode().toString(), InitializeStepNode.class);
    } catch (IOException e) {
      throw new InvalidYamlException(
          "Unable to parse initialize step yaml. Please ensure that it is in correct format", e);
    }
  }

  @Override
  public Set<String> getSupportedYamlVersions() {
    return Set.of(HarnessYamlVersion.V1);
  }
}
