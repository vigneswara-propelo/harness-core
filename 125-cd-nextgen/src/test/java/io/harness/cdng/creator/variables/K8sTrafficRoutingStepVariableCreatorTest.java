/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.variables;

import static io.harness.rule.OwnerRule.MLUKIC;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.k8s.K8sTrafficRoutingStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDP)
public class K8sTrafficRoutingStepVariableCreatorTest extends CategoryTest {
  private final K8sTrafficRoutingStepVariableCreator k8sTrafficRoutingStepVariableCreator =
      new K8sTrafficRoutingStepVariableCreator();

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testGetSupportedTypes() {
    assertThat(k8sTrafficRoutingStepVariableCreator.getSupportedStepTypes())
        .isEqualTo(Collections.singleton(StepSpecTypeConstants.K8S_TRAFFIC_ROUTING));
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testGetFieldClass() {
    assertThat(k8sTrafficRoutingStepVariableCreator.getFieldClass()).isEqualTo(K8sTrafficRoutingStepNode.class);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testCreateVariablesForParentNodeV2() throws IOException {
    List<String> fqnPropertiesList = StepVariableCreatorTestUtils.getFqnPropertiesForParentNodeV2(
        "cdng/variables/pipelineWithK8sTrafficRoutingStep.json", k8sTrafficRoutingStepVariableCreator,
        K8sTrafficRoutingStepNode.class);

    assertThat(fqnPropertiesList)
        .containsOnly("pipeline.stages.K8s.spec.execution.steps.K8s_Traffic_Routing_Step.description",
            "pipeline.stages.K8s.spec.execution.steps.K8s_Traffic_Routing_Step.timeout",
            "pipeline.stages.K8s.spec.execution.steps.K8s_Traffic_Routing_Step.spec.delegateSelectors",
            "pipeline.stages.K8s.spec.execution.steps.K8s_Traffic_Routing_Step.name",
            "pipeline.stages.K8s.spec.execution.steps.K8s_Traffic_Routing_Step.when");
  }
}