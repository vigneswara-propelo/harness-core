/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.pms.execution.strategy;

import static io.harness.rule.OwnerRule.SHALINI;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanService;
import io.harness.engine.pms.data.PmsEngineExpressionService;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.engine.pms.execution.strategy.plannode.PlanNodeExecutionStrategy;
import io.harness.execution.NodeExecution;
import io.harness.expression.common.ExpressionMode;
import io.harness.plan.PlanNode;
import io.harness.plancreator.exports.ExportConfig;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.data.StepOutcomeRef;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.contracts.steps.io.StepOutcomeProto;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@OwnedBy(HarnessTeam.PIPELINE)
@RunWith(MockitoJUnitRunner.class)
public class EndNodeExecutionHelperTest extends OrchestrationTestBase {
  @Mock private PmsOutcomeService pmsOutcomeService;
  @Mock private NodeExecutionService nodeExecutionService;
  @Mock private PlanNodeExecutionStrategy executionStrategy;
  @Mock private PlanService planService;
  @Mock private PmsEngineExpressionService pmsEngineExpressionService;
  @InjectMocks @Spy EndNodeExecutionHelper endNodeExecutionHelper;

  @Test
  @Owner(developers = SHALINI)
  @Category(UnitTests.class)
  public void testEndNodeExecutionWithNoAdvisers() {
    doReturn(null)
        .when(endNodeExecutionHelper)
        .processStepResponseWithNoAdvisers(any(Ambiance.class), any(StepResponseProto.class));
    endNodeExecutionHelper.endNodeExecutionWithNoAdvisers(
        Ambiance.newBuilder().build(), StepResponseProto.newBuilder().build());
    verify(executionStrategy, times(0)).endNodeExecution(any(Ambiance.class));
    doReturn(NodeExecution.builder().ambiance(Ambiance.newBuilder().build()).build())
        .when(endNodeExecutionHelper)
        .processStepResponseWithNoAdvisers(any(Ambiance.class), any(StepResponseProto.class));
    endNodeExecutionHelper.endNodeExecutionWithNoAdvisers(
        Ambiance.newBuilder().build(), StepResponseProto.newBuilder().build());
    verify(executionStrategy, times(1)).endNodeExecution(any(Ambiance.class));
  }

  @Test
  @Owner(developers = SHALINI)
  @Category(UnitTests.class)
  public void testProcessStepResponseWithNoAdvisers() {
    NodeExecution nodeExecution = NodeExecution.builder().build();
    doReturn(nodeExecution)
        .when(nodeExecutionService)
        .updateStatusWithOps(anyString(), any(Status.class), any(Consumer.class), any(EnumSet.class));
    assertThat(endNodeExecutionHelper.processStepResponseWithNoAdvisers(
                   Ambiance.newBuilder().addLevels(Level.newBuilder().setRuntimeId("1").build()).build(),
                   StepResponseProto.newBuilder().setStatus(Status.valueOf("NO_OP")).build()))
        .isInstanceOf(NodeExecution.class);
  }

  @Test
  @Owner(developers = SHALINI)
  @Category(UnitTests.class)
  public void testHandleOutcomes() {
    // CurrentLevel is of pipeline(not stage or stepGroup) so exports will not be published.
    Ambiance ambiance =
        Ambiance.newBuilder()
            .setPlanId(UUIDGenerator.generateUuid())
            .addLevels(Level.newBuilder()
                           .setSetupId(UUIDGenerator.generateUuid())
                           .setStepType(StepType.newBuilder().setStepCategory(StepCategory.PIPELINE).build())
                           .build())
            .build();
    assertEquals(
        new ArrayList<>(), endNodeExecutionHelper.handleOutcomes(ambiance, new ArrayList<>(), new ArrayList<>()));
    StepOutcomeProto stepOutcomeProto =
        StepOutcomeProto.newBuilder().setOutcome("1").setName("proto").setGroup("group1").build();
    doReturn("id1").when(pmsOutcomeService).consume(any(Ambiance.class), eq("proto"), anyString(), anyString());
    List<StepOutcomeProto> stepOutcomeProtoList = new ArrayList<>();
    stepOutcomeProtoList.add(stepOutcomeProto);
    List<StepOutcomeRef> stepOutcomeRefs =
        endNodeExecutionHelper.handleOutcomes(ambiance, stepOutcomeProtoList, new ArrayList<>());
    assertEquals(1, stepOutcomeRefs.size());
    assertEquals("id1", stepOutcomeRefs.get(0).getInstanceId());
    assertEquals("proto", stepOutcomeRefs.get(0).getName());

    // CurrentLevel is of stage but planNode does not have any exports so exports will not be published.
    ambiance = ambiance.toBuilder()
                   .addLevels(Level.newBuilder()
                                  .setSetupId(UUIDGenerator.generateUuid())
                                  .setStepType(StepType.newBuilder().setStepCategory(StepCategory.STAGE).build())
                                  .build())
                   .build();
    doReturn(PlanNode.builder().build())
        .when(planService)
        .fetchNode(ambiance.getPlanId(), AmbianceUtils.obtainCurrentSetupId(ambiance));
    stepOutcomeRefs = endNodeExecutionHelper.handleOutcomes(ambiance, stepOutcomeProtoList, new ArrayList<>());
    assertEquals(1, stepOutcomeRefs.size());
    assertEquals("id1", stepOutcomeRefs.get(0).getInstanceId());
    assertEquals("proto", stepOutcomeRefs.get(0).getName());

    // Now the planNode has populated the exports so exports outcomes will be published.
    Map<String, ExportConfig> exportConfigMap =
        Map.of("export_1", ExportConfig.builder().value("<+some.expression>").desc("Description").build());
    doReturn(PlanNode.builder().exports(exportConfigMap).build())
        .when(planService)
        .fetchNode(ambiance.getPlanId(), AmbianceUtils.obtainCurrentSetupId(ambiance));
    doReturn(Map.of("export_1", ExportConfig.builder().value("resolved_expression_value").desc("Description").build()))
        .when(pmsEngineExpressionService)
        .resolve(ambiance, exportConfigMap, ExpressionMode.RETURN_ORIGINAL_EXPRESSION_IF_UNRESOLVED);

    doReturn("id2")
        .when(pmsOutcomeService)
        .consume(any(Ambiance.class), eq(YAMLFieldNameConstants.EXPORTS), anyString(), anyString());
    stepOutcomeRefs = endNodeExecutionHelper.handleOutcomes(ambiance, stepOutcomeProtoList, new ArrayList<>());
    assertEquals(2, stepOutcomeRefs.size());
    assertEquals("id1", stepOutcomeRefs.get(0).getInstanceId());
    assertEquals("proto", stepOutcomeRefs.get(0).getName());

    assertEquals("id2", stepOutcomeRefs.get(1).getInstanceId());
    assertEquals(YAMLFieldNameConstants.EXPORTS, stepOutcomeRefs.get(1).getName());
  }

  @Test
  @Owner(developers = SHALINI)
  @Category(UnitTests.class)
  public void testHandleStepResponsePreAdviser() {
    doReturn(null)
        .when(endNodeExecutionHelper)
        .processStepResponsePreAdvisers(any(Ambiance.class), any(StepResponseProto.class));
    assertNull(endNodeExecutionHelper.handleStepResponsePreAdviser(
        Ambiance.newBuilder().build(), StepResponseProto.newBuilder().build()));
  }

  @Test
  @Owner(developers = SHALINI)
  @Category(UnitTests.class)
  public void testProcessStepResponsePreAdvisers() {
    NodeExecution nodeExecution = NodeExecution.builder().build();
    doReturn(nodeExecution)
        .when(nodeExecutionService)
        .updateStatusWithOps(anyString(), any(Status.class), any(Consumer.class), any(EnumSet.class));
    assertThat(endNodeExecutionHelper.processStepResponseWithNoAdvisers(
                   Ambiance.newBuilder().addLevels(Level.newBuilder().setRuntimeId("1").build()).build(),
                   StepResponseProto.newBuilder().setStatus(Status.valueOf("NO_OP")).build()))
        .isInstanceOf(NodeExecution.class);
  }
}
