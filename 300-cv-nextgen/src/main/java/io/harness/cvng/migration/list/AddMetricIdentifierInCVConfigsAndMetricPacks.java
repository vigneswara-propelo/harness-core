/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.migration.list;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricCVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.MetricPack.MetricPackKeys;
import io.harness.cvng.migration.CVNGMigration;
import io.harness.cvng.migration.beans.ChecklistItem;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
public class AddMetricIdentifierInCVConfigsAndMetricPacks implements CVNGMigration {
  @Inject private HPersistence hPersistence;

  @Override
  public void migrate() {
    log.info("Deleting Metric Packs for Old Custom Health data source type");
    hPersistence.deleteOnServer(
        hPersistence.createQuery(MetricPack.class).filter(MetricPackKeys.dataSourceType, "CUSTOM_HEALTH"));
    log.info("Deleting cvCOnfigs for Old Custom Health data source type");
    hPersistence.deleteOnServer(hPersistence.createQuery(CVConfig.class)
                                    .disableValidation()
                                    .filter("className", "io.harness.cvng.core.entities.CustomHealthCVConfig"));

    log.info("Adding identifiers for Metric Packs");
    Query<MetricPack> metricPackQuery = hPersistence.createQuery(MetricPack.class);
    try (HIterator<MetricPack> iterator = new HIterator<>(metricPackQuery.fetch())) {
      while (iterator.hasNext()) {
        MetricPack metricPack = iterator.next();

        metricPack.getMetrics().forEach(metricDefinition -> {
          if (isEmpty(metricDefinition.getIdentifier())) {
            String identifier = metricDefinition.getName().replaceAll(" ", "_");
            identifier = identifier.replaceAll("\\(", "");
            identifier = identifier.replaceAll("\\)", "");
            metricDefinition.setIdentifier(identifier);
          }
        });
        hPersistence.save(metricPack);
        log.info("Identifier updation for metric pack {}, {}", metricPack.getProjectIdentifier(),
            metricPack.getIdentifier());
      }
    }

    log.info("Adding identifiers for metric pack in cvconfigs");
    Query<MetricCVConfig> metricCVConfigQuery = hPersistence.createQuery(MetricCVConfig.class);
    try (HIterator<MetricCVConfig> iterator = new HIterator<>(metricCVConfigQuery.fetch())) {
      while (iterator.hasNext()) {
        MetricCVConfig cvConfig = iterator.next();

        if (cvConfig.getMetricPack() != null && cvConfig.getMetricPack().getMetrics() != null) {
          cvConfig.getMetricPack().getMetrics().forEach(metricDefinition -> {
            if (metricDefinition != null && isEmpty(metricDefinition.getIdentifier())) {
              String identifier = metricDefinition.getName().replaceAll(" ", "_");
              identifier = identifier.replaceAll("\\(", "");
              identifier = identifier.replaceAll("\\)", "");
              metricDefinition.setIdentifier(identifier);
            }
          });
        }
        hPersistence.save(cvConfig);
        log.info("Identifier updation for cvConfig {}, {}", cvConfig.getProjectIdentifier(), cvConfig.getUuid());
      }
    }
  }

  @Override
  public ChecklistItem whatHappensOnRollback() {
    return ChecklistItem.NA;
  }

  @Override
  public ChecklistItem whatHappensIfOldVersionIteratorPicksMigratedEntity() {
    return ChecklistItem.NA;
  }
}
