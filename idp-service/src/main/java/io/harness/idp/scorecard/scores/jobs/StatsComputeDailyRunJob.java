/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.scores.jobs;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.scorecard.scores.service.StatsComputeService;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.dropwizard.lifecycle.Managed;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.IDP)
public class StatsComputeDailyRunJob implements Managed {
  private ScheduledExecutorService executorService;
  private final StatsComputeService statsComputeService;

  @Inject
  public StatsComputeDailyRunJob(@Named("statsComputeDailyRunJob") ScheduledExecutorService executorService,
      StatsComputeService statsComputeService) {
    this.executorService = executorService;
    this.statsComputeService = statsComputeService;
  }

  @Override
  public void start() throws Exception {
    executorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("stats-compute-daily-run-job").build());
    long midnight = LocalDateTime.now().until(LocalDate.now().plusDays(1).atStartOfDay(), ChronoUnit.MINUTES);
    log.info("Scheduling StatsComputeDailyRunJob with initial delay of {} minutes from current time", midnight);
    executorService.scheduleAtFixedRate(this::run, midnight + 10, TimeUnit.DAYS.toMinutes(1), TimeUnit.MINUTES);
  }

  @Override
  public void stop() throws Exception {
    executorService.shutdownNow();
    if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
      log.error("StatsComputeDailyRunJob executorService terminated after the timeout of 30 seconds");
    }
  }

  public void run() {
    log.info("Stats compute daily run job started");
    try {
      statsComputeService.populateStatsData();
    } catch (Exception ex) {
      log.error("Error in StatsComputeDailyRunJob. Error = {}", ex.getMessage(), ex);
      throw ex;
    }
    log.info("Stats compute daily run job completed");
  }
}
