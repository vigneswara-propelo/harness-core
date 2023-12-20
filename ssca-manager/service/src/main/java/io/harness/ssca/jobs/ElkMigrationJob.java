/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.jobs;

import static io.harness.ssca.entities.migration.MigrationEntity.MigrationStatus.FAILURE;
import static io.harness.ssca.entities.migration.MigrationEntity.MigrationStatus.SUCCESS;
import static io.harness.ssca.search.utils.SSCAMigrationConstants.JOB_NAME;

import io.harness.exception.GeneralException;
import io.harness.repositories.MigrationRepo;
import io.harness.ssca.entities.migration.MigrationEntity;
import io.harness.ssca.search.ElkMigrationService;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ElkMigrationJob implements Managed {
  private final ElkMigrationService elkMigrationService;
  private Future<?> elkMigrationJobFuture;

  private MigrationRepo migrationRepo;
  private static final String ELK_DEBUG_LOG = "[ElkMigrationJob]: ";

  @Inject
  public ElkMigrationJob(ElkMigrationService elkMigrationService, MigrationRepo migrationRepo) {
    this.elkMigrationService = elkMigrationService;
    this.migrationRepo = migrationRepo;
  }

  @Override
  public void start() throws Exception {
    Optional<MigrationEntity> migrationEntityOptional = migrationRepo.findByName(JOB_NAME);
    if (migrationEntityOptional.isPresent()) {
      MigrationEntity migrationEntity = migrationEntityOptional.get();
      if (SUCCESS.equals(migrationEntity.getStatus())) {
        log.info(ELK_DEBUG_LOG + "Elasticsearch migration is complete. Skipping ELK migration job...");
        return;
      }
    }
    log.info(ELK_DEBUG_LOG + "Starting ELK migration job...");
    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("elk-migration-job-thread").build());
    elkMigrationJobFuture = executorService.schedule(this::migrate, 0, TimeUnit.SECONDS);
  }

  @Override
  public void stop() throws Exception {
    if (elkMigrationJobFuture != null) {
      log.info(ELK_DEBUG_LOG + "Stopping ELK migration job...");
      elkMigrationJobFuture.cancel(false);
    }
  }

  private void migrate() {
    log.info(ELK_DEBUG_LOG + "Starting ELK migration....");

    try {
      elkMigrationService.run();
    } catch (Exception e) {
      log.error(ELK_DEBUG_LOG + "ELK migration failed....", e);
      migrationRepo.save(MigrationEntity.builder().name(JOB_NAME).status(FAILURE).build());
      throw new GeneralException("Elk migration failed");
    }
  }
}
