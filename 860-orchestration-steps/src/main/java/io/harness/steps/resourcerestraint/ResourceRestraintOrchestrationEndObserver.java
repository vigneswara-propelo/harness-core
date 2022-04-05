package io.harness.steps.resourcerestraint;

import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.observers.OrchestrationEndObserver;
import io.harness.logging.AutoLogContext;
import io.harness.observer.AsyncInformObserver;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;
import io.harness.steps.resourcerestraint.service.ResourceRestraintInstanceService;

import com.google.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResourceRestraintOrchestrationEndObserver implements OrchestrationEndObserver, AsyncInformObserver {
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  @Inject private ResourceRestraintInstanceService restraintService;

  @Override
  public void onEnd(Ambiance ambiance) {
    try (AutoLogContext ignore = AmbianceUtils.autoLogContext(ambiance)) {
      log.info("Update Active Resource constraints");
      final List<ResourceRestraintInstance> restraintInstances =
          restraintService.findAllActiveAndBlockedByReleaseEntityId(ambiance.getPlanExecutionId());

      log.info("Found {} active resource restraint instances", restraintInstances.size());
      if (EmptyPredicate.isNotEmpty(restraintInstances)) {
        for (ResourceRestraintInstance ri : restraintInstances) {
          restraintService.processRestraint(ri);
        }
      }
      log.info("Updated Blocked Resource constraints");
    } catch (RuntimeException exception) {
      // Do not block the execution for possible exception in the RC update
      log.error("Something wrong with resource constraints update", exception);
    }
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }
}
