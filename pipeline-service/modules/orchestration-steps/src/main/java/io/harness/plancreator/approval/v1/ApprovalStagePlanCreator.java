/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.plancreator.approval.v1;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidYamlException;
import io.harness.plancreator.stages.v1.AbstractStagePlanCreator;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.steps.approval.stage.v1.ApprovalStageNodeV1;

import java.io.IOException;
import java.util.Collections;
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
    return null;
  }
  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, ApprovalStageNodeV1 field, List<String> childrenNodeIds) {
    return null;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(
        YAMLFieldNameConstants.STAGE, Collections.singleton(YAMLFieldNameConstants.APPROVAL_V1));
  }
}
