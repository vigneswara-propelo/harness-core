/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.ssca.entities.ScorecardEntity;
import io.harness.ssca.entities.ScorecardEntity.ScorecardEntityKeys;

import com.google.inject.Inject;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class ScorecardRepoImpl implements ScorecardRepo {
  @Inject MongoTemplate mongoTemplate;

  @Override
  public void save(ScorecardEntity scorecardEntity) {
    if (validateEntity(scorecardEntity)) {
      mongoTemplate.save(scorecardEntity, "sbomScorecard");
    }
  }

  @Override
  public ScorecardEntity getByOrchestrationId(
      String accountId, String orgId, String projectId, String orchestrationId) {
    if (isNotEmpty(accountId) && isNotEmpty(orgId) && isNotEmpty(projectId) && isNotEmpty(orchestrationId)) {
      Criteria criteria = Criteria.where(ScorecardEntityKeys.accountId)
                              .is(accountId)
                              .and(ScorecardEntityKeys.orgId)
                              .is(orgId)
                              .and(ScorecardEntityKeys.projectId)
                              .is(projectId)
                              .and(ScorecardEntityKeys.orchestrationId)
                              .is(orchestrationId);

      Query query = new Query(criteria);

      return mongoTemplate.findOne(query, ScorecardEntity.class);
    }
    return null;
  }

  private boolean validateEntity(ScorecardEntity scorecardEntity) {
    return scorecardEntity != null && isNotEmpty(scorecardEntity.getAccountId())
        && isNotEmpty(scorecardEntity.getOrgId()) && isNotEmpty(scorecardEntity.getProjectId())
        && isNotEmpty(scorecardEntity.getOrchestrationId()) && scorecardEntity.getSbom() != null
        && scorecardEntity.getScorecardInfo() != null && isNotEmpty(scorecardEntity.getScores());
  }
}
