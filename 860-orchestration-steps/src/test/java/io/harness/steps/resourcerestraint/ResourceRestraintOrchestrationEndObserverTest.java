package io.harness.steps.resourcerestraint;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.distribution.constraint.Consumer.State.ACTIVE;
import static io.harness.distribution.constraint.Consumer.State.BLOCKED;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationStepsTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.rule.Owner;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;
import io.harness.steps.resourcerestraint.service.ResourceRestraintInstanceService;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ResourceRestraintOrchestrationEndObserverTest extends OrchestrationStepsTestBase {
  @Mock ResourceRestraintInstanceService restraintInstanceService;
  @Inject @InjectMocks ResourceRestraintOrchestrationEndObserver observer;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestOnEnd() {
    String planExecutionId = generateUuid();
    Ambiance ambiance = Ambiance.newBuilder().setPlanExecutionId(planExecutionId).build();
    ResourceRestraintInstance activeRc =
        ResourceRestraintInstance.builder().resourceRestraintId(generateUuid()).state(ACTIVE).build();
    ResourceRestraintInstance blockedRc =
        ResourceRestraintInstance.builder().resourceRestraintId(generateUuid()).state(BLOCKED).build();

    when(restraintInstanceService.findAllActiveAndBlockedByReleaseEntityId(eq(planExecutionId)))
        .thenReturn(ImmutableList.of(activeRc, blockedRc));
    observer.onEnd(ambiance);

    ArgumentCaptor<ResourceRestraintInstance> instanceCaptor = ArgumentCaptor.forClass(ResourceRestraintInstance.class);

    verify(restraintInstanceService, times(2)).processRestraint(instanceCaptor.capture());

    List<ResourceRestraintInstance> values = instanceCaptor.getAllValues();
    assertThat(values).hasSize(2);
    assertThat(values).containsExactlyInAnyOrder(activeRc, blockedRc);
  }
}