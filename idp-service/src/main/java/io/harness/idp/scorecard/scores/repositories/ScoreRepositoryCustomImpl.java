/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.idp.scorecard.scores.repositories;

import static io.harness.idp.common.DateUtils.getPreviousDay24HourTimeFrame;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.scorecard.scores.entity.ScoreEntity;
import io.harness.spec.server.idp.v1.model.CheckStatus;

import com.google.inject.Inject;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.IDP)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ScoreRepositoryCustomImpl implements ScoreRepositoryCustom {
  private MongoTemplate mongoTemplate;
  @Override
  public AggregationResults<ScoreEntityByScorecardIdentifier> getAllLatestScoresByScorecardsForAnEntity(
      String accountIdentifier, String entityIdentifier) {
    Criteria criteria = Criteria.where(ScoreEntity.ScoreKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(ScoreEntity.ScoreKeys.entityIdentifier)
                            .is(entityIdentifier);

    ProjectionOperation projectionOperation = Aggregation.project()
                                                  .andExpression(Constants.ID_KEY)
                                                  .as(ScoreEntity.ScoreKeys.scorecardIdentifier)
                                                  .andExpression(Constants.SCORE_ENTITY_KEY)
                                                  .as(Constants.SCORE_ENTITY_KEY);

    Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria),
        Aggregation.sort(Sort.Direction.DESC, ScoreEntity.ScoreKeys.lastComputedTimestamp),
        Aggregation.group(ScoreEntity.ScoreKeys.scorecardIdentifier)
            .push(ScoreEntity.ScoreKeys.scorecardIdentifier)
            .as(ScoreEntity.ScoreKeys.scorecardIdentifier)
            .first(Aggregation.ROOT)
            .as(Constants.SCORE_ENTITY_KEY),
        projectionOperation);

    AggregationResults<ScoreEntityByScorecardIdentifier> result =
        mongoTemplate.aggregate(aggregation, Constants.SCORE_COLLECTION_NAME, ScoreEntityByScorecardIdentifier.class);
    return result;
  }

  @Override
  public ScoreEntity getLatestComputedScoreForEntityAndScorecard(
      String accountIdentifier, String entityIdentifier, String scoreCardIdentifier) {
    Criteria criteria = Criteria.where(ScoreEntity.ScoreKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(ScoreEntity.ScoreKeys.entityIdentifier)
                            .is(entityIdentifier)
                            .and(ScoreEntity.ScoreKeys.scorecardIdentifier)
                            .is(scoreCardIdentifier);
    Query query =
        new Query(criteria).with(Sort.by(Sort.Direction.DESC, ScoreEntity.ScoreKeys.lastComputedTimestamp)).limit(1);
    return mongoTemplate.findOne(query, ScoreEntity.class);
  }

  @Override
  public List<ScoreEntityByEntityIdentifier> getLatestScoresForScorecard(
      String accountIdentifier, String scorecardIdentifier) {
    Pair<Long, Long> previousDay24HourTimeFrame = getPreviousDay24HourTimeFrame();
    Criteria criteria = Criteria.where(ScoreEntity.ScoreKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(ScoreEntity.ScoreKeys.scorecardIdentifier)
                            .is(scorecardIdentifier)
                            .and(ScoreEntity.ScoreKeys.lastComputedTimestamp)
                            .gt(previousDay24HourTimeFrame.getLeft())
                            .lt(previousDay24HourTimeFrame.getRight());

    ProjectionOperation projectionOperation = Aggregation.project()
                                                  .andExpression(Constants.ID_KEY)
                                                  .as(ScoreEntity.ScoreKeys.entityIdentifier)
                                                  .andExpression(Constants.SCORE_ENTITY_KEY)
                                                  .as(Constants.SCORE_ENTITY_KEY);

    Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria),
        Aggregation.sort(Sort.Direction.DESC, ScoreEntity.ScoreKeys.lastComputedTimestamp),
        Aggregation.group(ScoreEntity.ScoreKeys.entityIdentifier)
            .push(ScoreEntity.ScoreKeys.entityIdentifier)
            .as(ScoreEntity.ScoreKeys.entityIdentifier)
            .first(Aggregation.ROOT)
            .as(Constants.SCORE_ENTITY_KEY),
        projectionOperation);
    return mongoTemplate.aggregate(aggregation, Constants.SCORE_COLLECTION_NAME, ScoreEntityByEntityIdentifier.class)
        .getMappedResults();
  }

  @Override
  public UpdateResult updateCheckIdentifier(ScoreEntity score, List<CheckStatus> checkStatuses) {
    Criteria criteria = Criteria.where(ScoreEntity.ScoreKeys.accountIdentifier)
                            .is(score.getAccountIdentifier())
                            .and(ScoreEntity.ScoreKeys.scorecardIdentifier)
                            .is(score.getScorecardIdentifier())
                            .and(ScoreEntity.ScoreKeys.entityIdentifier)
                            .is(score.getEntityIdentifier())
                            .and(ScoreEntity.ScoreKeys.lastComputedTimestamp)
                            .is(score.getLastComputedTimestamp());
    Query query = new Query(criteria);
    Update update = new Update();
    update.set(ScoreEntity.ScoreKeys.checkStatus, checkStatuses);
    return mongoTemplate.updateFirst(query, update, ScoreEntity.class);
  }
}
