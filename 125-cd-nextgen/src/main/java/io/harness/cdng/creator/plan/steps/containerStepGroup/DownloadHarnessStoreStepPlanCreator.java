/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.steps.containerStepGroup;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.containerStepGroup.DownloadHarnessStoreStepNode;
import io.harness.cdng.containerStepGroup.DownloadHarnessStoreStepParameters;
import io.harness.cdng.creator.plan.steps.CDPMSStepPlanCreatorV2;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import com.google.common.collect.Sets;
import java.util.Set;

@OwnedBy(HarnessTeam.CDP)
public class DownloadHarnessStoreStepPlanCreator extends CDPMSStepPlanCreatorV2<DownloadHarnessStoreStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.DOWNLOAD_HARNESS_STORE);
  }

  @Override
  public Class<DownloadHarnessStoreStepNode> getFieldClass() {
    return DownloadHarnessStoreStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, DownloadHarnessStoreStepNode stepNode) {
    return super.createPlanForField(ctx, stepNode);
  }

  @Override
  protected StepParameters getStepParameters(PlanCreationContext ctx, DownloadHarnessStoreStepNode stepNode) {
    final StepParameters stepParameters = super.getStepParameters(ctx, stepNode);
    DownloadHarnessStoreStepParameters downloadHarnessStoreStepParameters =
        (DownloadHarnessStoreStepParameters) ((StepElementParameters) stepParameters).getSpec();
    downloadHarnessStoreStepParameters.setDelegateSelectors(
        stepNode.getDownloadHarnessStoreStepInfo().getDelegateSelectors());
    return stepParameters;
  }
}
