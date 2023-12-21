/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.steps;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.cdng.k8s.K8sTrafficRoutingStepNode;
import io.harness.cdng.k8s.asyncsteps.K8sTrafficRoutingStep;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import com.google.common.collect.Sets;
import java.util.Set;

@OwnedBy(CDP)
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_K8S})
public class K8sTrafficRoutingStepPlanCreator extends CDPMSStepPlanCreatorV2<K8sTrafficRoutingStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.K8S_TRAFFIC_ROUTING);
  }

  @Override
  public Class<K8sTrafficRoutingStepNode> getFieldClass() {
    return K8sTrafficRoutingStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, K8sTrafficRoutingStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }

  @Override
  protected StepParameters getStepParameters(PlanCreationContext ctx, K8sTrafficRoutingStepNode stepElement) {
    return super.getStepParameters(ctx, stepElement);
  }

  @Override
  public StepType getStepSpecType(PlanCreationContext ctx, K8sTrafficRoutingStepNode stepElement) {
    return K8sTrafficRoutingStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType(PlanCreationContext ctx, K8sTrafficRoutingStepNode stepElement) {
    return OrchestrationFacilitatorType.ASYNC;
  }
}
