/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.jobs;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.harness.SSCAIteratorConfig;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import io.harness.ssca.entities.remediation_tracker.RemediationStatus;
import io.harness.ssca.entities.remediation_tracker.RemediationTrackerEntity;
import io.harness.ssca.entities.remediation_tracker.RemediationTrackerEntity.RemediationTrackerEntityKeys;
import io.harness.ssca.services.remediation_tracker.RemediationTrackerService;

import com.google.inject.Inject;
import javax.validation.constraints.NotNull;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

public class RemediationTrackerUpdateArtifactsIteratorHandler
    implements MongoPersistenceIterator.Handler<RemediationTrackerEntity> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;

  @Inject private MongoTemplate mongoTemplate;

  @Inject private RemediationTrackerService remediationTrackerService;

  public void registerIterators(SSCAIteratorConfig config) {
    if (config == null || !config.isEnabled()) {
      return;
    }
    SpringFilterExpander filterExpander = getFilterQuery();
    registerIteratorWithFactory(config, filterExpander);
  }

  private void registerIteratorWithFactory(SSCAIteratorConfig config, @NotNull SpringFilterExpander filterExpander) {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name(this.getClass().getName())
            .poolSize(config.getThreadPoolSize())
            .interval(ofMinutes(5))
            .build(),
        RemediationTrackerEntity.class,
        MongoPersistenceIterator.<RemediationTrackerEntity, SpringFilterExpander>builder()
            .clazz(RemediationTrackerEntity.class)
            .fieldName(RemediationTrackerEntityKeys.nextIteration)
            .acceptableNoAlertDelay(ofMinutes(5))
            .targetInterval(ofSeconds(config.getTargetIntervalInSeconds()))
            .filterExpander(filterExpander)
            .acceptableExecutionTime(
                ofSeconds(5)) // This is the time we expect the processing to complete if it takes more we will log.
            // Corrective action here is to see why things are slow and either increase the param
            // time or correct business logic.
            .acceptableNoAlertDelay(ofMinutes(5)) // This is only used to log delays. This log signifies that the
            // records became eligible long time ago before the thread
            // actually came here to process. If we are encountering this log
            // it means our thread pool must be increased for the iterator.
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }

  private SpringFilterExpander getFilterQuery() {
    return query -> {
      Criteria criteria = Criteria.where(RemediationTrackerEntityKeys.status).is(RemediationStatus.ON_GOING);
      query.addCriteria(criteria);
    };
  }

  @Override
  public void handle(RemediationTrackerEntity entity) {
    remediationTrackerService.updateArtifactsAndEnvironments(entity);
  }
}
