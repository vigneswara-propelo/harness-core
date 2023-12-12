/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.migration;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;
import io.harness.repositories.EnforcementSummaryRepo;
import io.harness.ssca.entities.EnforcementSummaryEntity;
import io.harness.ssca.entities.EnforcementSummaryEntity.EnforcementSummaryEntityKeys;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Slf4j
@OwnedBy(HarnessTeam.SSCA)
public class RemoveDuplicateIdFromEnforcementSummary implements NGMigration {
  @Inject MongoTemplate mongoTemplate;
  @Inject EnforcementSummaryRepo enforcementSummaryRepo;
  private static final String DEBUG_LOG = "REMOVE_DUPLICATE_ENFORCEMENT_MIGRATION: ";
  @Override
  public void migrate() {
    log.info(DEBUG_LOG + "Starting migration to remove duplicate enforcementId");
    Criteria criteria = Criteria.where("count").gt(1);
    GroupOperation groupByOperation = group(EnforcementSummaryEntityKeys.enforcementId).count().as("count");
    MatchOperation matchOperation = match(criteria);

    Aggregation aggregation = Aggregation.newAggregation(groupByOperation, matchOperation);
    List<Map> results =
        mongoTemplate.aggregate(aggregation, EnforcementSummaryEntity.class, Map.class).getMappedResults();
    for (Map result : results) {
      String enforcementId = null;
      try {
        enforcementId = result.get("_id").toString();

        Query query = new Query()
                          .addCriteria(Criteria.where(EnforcementSummaryEntityKeys.enforcementId).is(enforcementId))
                          .with(Sort.by(Sort.Direction.DESC, EnforcementSummaryEntityKeys.createdAt));
        List<EnforcementSummaryEntity> entities = mongoTemplate.find(query, EnforcementSummaryEntity.class);
        entities.remove(0);
        enforcementSummaryRepo.deleteAll(entities);
      } catch (Exception e) {
        log.error(String.format("Remove Duplicates failed for enforcement id [%s], Exception: %s", enforcementId, e));
      }
    }
    log.info(DEBUG_LOG + "Migration to remove duplicate enforcementId completed");
  }
}
