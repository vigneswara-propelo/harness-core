/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.INDER;

import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.tasks.ResponseData;

import software.wings.WingsBaseTest;
import software.wings.beans.ArtifactCollectLoopParams;
import software.wings.beans.artifact.ArtifactInput;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatusData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstanceHelper;
import software.wings.sm.StateType;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

@OwnedBy(CDC)
public class ArtifactCollectLoopStateTest extends WingsBaseTest {
  private static final String ARTIFACT_STREAM_ID_1 = "ARTIFACT_STREAM_ID_1";
  private static final String ARTIFACT_STREAM_ID_2 = "ARTIFACT_STREAM_ID_2";
  @Mock private StateExecutionInstanceHelper instanceHelper;
  @Mock private ExecutionContextImpl context;

  @InjectMocks
  @Spy
  private ArtifactCollectLoopState artifactCollectLoopState =
      new ArtifactCollectLoopState("ARTIFACT_COLLECT_LOOP_STATE");

  @Before
  public void setUp() {
    ArtifactInput artifactInput1 =
        ArtifactInput.builder().buildNo("1.0").artifactStreamId(ARTIFACT_STREAM_ID_1).build();
    ArtifactInput artifactInput2 =
        ArtifactInput.builder().buildNo("2.0").artifactStreamId(ARTIFACT_STREAM_ID_2).build();
    artifactCollectLoopState.setArtifactInputList(asList(artifactInput1, artifactInput2));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testExecute() {
    StateExecutionInstance stateExecutionInstance = aStateExecutionInstance().uuid("UUID").build();
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);
    when(instanceHelper.clone(stateExecutionInstance)).thenReturn(stateExecutionInstance);

    ExecutionResponse executionResponse = artifactCollectLoopState.execute(context);
    verify(instanceHelper, times(2)).clone(stateExecutionInstance);
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.isAsync()).isEqualTo(true);
    assertThat(executionResponse.getCorrelationIds()).isNotEmpty().hasSize(2);
    assertThat(executionResponse.getStateExecutionInstances()).isNotEmpty().hasSize(2);
    StateExecutionInstance childStateExecutionInstance1 = executionResponse.getStateExecutionInstances().get(0);
    assertThat(childStateExecutionInstance1.getStateType()).isEqualTo(StateType.ARTIFACT_COLLECTION.getName());
    assertThat(childStateExecutionInstance1.isParentLoopedState()).isEqualTo(true);
    assertThat(childStateExecutionInstance1.getLoopedStateParams())
        .isIn(ArtifactCollectLoopParams.builder()
                  .artifactStreamId(ARTIFACT_STREAM_ID_1)
                  .buildNo("1.0")
                  .stepName("ARTIFACT_COLLECT_LOOP_STATE_1")
                  .build(),
            ArtifactCollectLoopParams.builder()
                .artifactStreamId(ARTIFACT_STREAM_ID_2)
                .buildNo("2.0")
                .stepName("ARTIFACT_COLLECT_LOOP_STATE_2")
                .build());
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseOneFailed() {
    Map<String, ResponseData> response = new HashMap<>();
    response.put("response1", ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build());
    response.put("response2", ExecutionStatusData.builder().executionStatus(ExecutionStatus.FAILED).build());

    ExecutionResponse executionResponse = artifactCollectLoopState.handleAsyncResponse(context, response);
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseAllSuccess() {
    Map<String, ResponseData> response = new HashMap<>();
    response.put("response1", ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build());
    response.put("response2", ExecutionStatusData.builder().executionStatus(ExecutionStatus.SUCCESS).build());
    doNothing().when(artifactCollectLoopState).updateArtifactsInContext(context);

    ExecutionResponse executionResponse = artifactCollectLoopState.handleAsyncResponse(context, response);
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    verify(artifactCollectLoopState).updateArtifactsInContext(context);
  }
}
