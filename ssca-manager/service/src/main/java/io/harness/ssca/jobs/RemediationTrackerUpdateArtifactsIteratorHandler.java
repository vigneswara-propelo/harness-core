/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.jobs;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofMinutes;

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

  public void registerIterators(int threadPoolSize) {
    SpringFilterExpander filterExpander = getFilterQuery();
    registerIteratorWithFactory(threadPoolSize, filterExpander);
  }

  private void registerIteratorWithFactory(int threadPoolSize, @NotNull SpringFilterExpander filterExpander) {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name(this.getClass().getName())
            .poolSize(threadPoolSize)
            .interval(ofMinutes(5))
            .build(),
        RemediationTrackerEntity.class,
        MongoPersistenceIterator.<RemediationTrackerEntity, SpringFilterExpander>builder()
            .clazz(RemediationTrackerEntity.class)
            .fieldName(RemediationTrackerEntityKeys.nextIteration)
            .acceptableNoAlertDelay(ofMinutes(5))
            .targetInterval(ofMinutes(30))
            .filterExpander(filterExpander)
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
    remediationTrackerService.updateArtifactsAndEnvironmentsInRemediationTracker(entity);
  }
}
