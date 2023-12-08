/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.servicelevelobjective.beans.jobs;

import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.servicelevelobjective.beans.SLIEvaluationType;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;
import io.harness.cvng.servicelevelobjective.services.api.SLIRecordService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.services.api.OrchestrationService;
import io.harness.mongo.iterator.MongoPersistenceIterator;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class MetricLessSLIStateMachineCreationHandler
    implements MongoPersistenceIterator.Handler<ServiceLevelIndicator> {
  @Inject OrchestrationService orchestrationService;
  @Inject VerificationTaskService verificationTaskService;
  @Inject Clock clock;

  @Inject SLIRecordService sliRecordService;
  @Override
  public void handle(ServiceLevelIndicator serviceLevelIndicator) {
    Optional<String> sliVerificationTaskId = verificationTaskService.getSLIVerificationTaskId(
        serviceLevelIndicator.getAccountId(), serviceLevelIndicator.getUuid());
    if (sliVerificationTaskId.isPresent()
        && SLIEvaluationType.METRIC_LESS.equals(serviceLevelIndicator.getSLIEvaluationType())) {
      SLIRecord latestSLIRecord = sliRecordService.getLatestSLIRecord(sliVerificationTaskId.get());
      Instant currentTime = DateTimeUtils.roundDownTo5MinBoundary(clock.instant());
      if (latestSLIRecord == null || latestSLIRecord.getTimestamp().plus(5, ChronoUnit.MINUTES).isBefore(currentTime)) {
        Instant endTime;
        if (latestSLIRecord != null) {
          currentTime = latestSLIRecord.getTimestamp().plus(1, ChronoUnit.MINUTES);
          endTime = currentTime.plus(5, ChronoUnit.MINUTES);
        } else {
          endTime = currentTime;
          currentTime = currentTime.minus(24, ChronoUnit.HOURS);
        }
        AnalysisInput analysisInput = AnalysisInput.builder()
                                          .verificationTaskId(sliVerificationTaskId.get())
                                          .startTime(currentTime)
                                          .endTime(endTime)
                                          .build();
        orchestrationService.queueAnalysis(analysisInput);
      }
    } else {
      throw new IllegalArgumentException("VerificationTaskId doesn't exist for SLI " + serviceLevelIndicator);
    }
  }
}
