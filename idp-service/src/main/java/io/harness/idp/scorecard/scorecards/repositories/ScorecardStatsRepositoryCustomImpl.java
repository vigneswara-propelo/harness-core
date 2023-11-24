/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.scorecards.repositories;

import static io.harness.idp.common.Constants.DOT_SEPARATOR;
import static io.harness.idp.common.DateUtils.yesterdayInMilliseconds;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.backstagebeans.BackstageCatalogEntity;
import io.harness.idp.backstagebeans.BackstageCatalogEntityTypes;
import io.harness.idp.scorecard.scorecards.beans.StatsMetadata;
import io.harness.idp.scorecard.scorecards.entity.ScorecardStatsEntity;
import io.harness.idp.scorecard.scorecards.entity.ScorecardStatsEntity.ScorecardStatsKeys;
import io.harness.idp.scorecard.scores.entity.ScoreEntity;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.IDP)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class ScorecardStatsRepositoryCustomImpl implements ScorecardStatsRepositoryCustom {
  private MongoTemplate mongoTemplate;
  private static final String ID_KEY = "_id";
  private static final String SCORE_KEY = "score";
  private static final String COUNT_KEY = "count";
  private static final String PERCENTAGE_KEY = "percentage";
  private static final String SCORES_GREATER_THAN_74_KEY = "scoresGreaterThan74";

  @Override
  public ScorecardStatsEntity findOneOrConstructStats(
      ScoreEntity scoreEntity, BackstageCatalogEntity backstageCatalog) {
    Criteria criteria = Criteria.where(ScorecardStatsKeys.accountIdentifier)
                            .is(scoreEntity.getAccountIdentifier())
                            .and(ScorecardStatsKeys.entityIdentifier)
                            .is(scoreEntity.getEntityIdentifier())
                            .and(ScorecardStatsKeys.scorecardIdentifier)
                            .is(scoreEntity.getScorecardIdentifier());
    ScorecardStatsEntity entity = mongoTemplate.findOne(Query.query(criteria), ScorecardStatsEntity.class);
    if (entity == null) {
      return ScorecardStatsEntity.builder()
          .accountIdentifier(scoreEntity.getAccountIdentifier())
          .entityIdentifier(scoreEntity.getEntityIdentifier())
          .scorecardIdentifier(scoreEntity.getScorecardIdentifier())
          .score(scoreEntity.getScore())
          .metadata(buildMetadata(backstageCatalog))
          .build();
    }
    entity.setScore(scoreEntity.getScore());
    entity.setMetadata(buildMetadata(backstageCatalog));
    entity.setLastUpdatedAt(yesterdayInMilliseconds());
    return entity;
  }

  @Override
  public List<ScorecardIdentifierAndScore> computeScoresPercentageByScorecard(
      String accountIdentifier, List<String> scorecardIdentifiers) {
    Criteria criteria = Criteria.where(ScorecardStatsKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(ScorecardStatsKeys.scorecardIdentifier)
                            .in(scorecardIdentifiers);

    ProjectionOperation projectionOperation =
        Aggregation.project()
            .andExpression(ID_KEY)
            .as(ScorecardStatsKeys.scorecardIdentifier)
            .andExpression(COUNT_KEY)
            .as(COUNT_KEY)
            .and(ConditionalOperators.when(Criteria.where(COUNT_KEY).ne(0))
                     .then(ArithmeticOperators.valueOf(SCORES_GREATER_THAN_74_KEY).divideBy(COUNT_KEY))
                     .otherwise(0))
            .as(PERCENTAGE_KEY);

    Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria),
        Aggregation.group(ScorecardStatsKeys.scorecardIdentifier, ScorecardStatsKeys.entityIdentifier)
            .first(SCORE_KEY)
            .as(SCORE_KEY),
        Aggregation.group(ID_KEY + DOT_SEPARATOR + ScorecardStatsKeys.scorecardIdentifier)
            .count()
            .as(COUNT_KEY)
            .sum(ConditionalOperators.when(Criteria.where(SCORE_KEY).gt(74)).then(1).otherwise(0))
            .as(SCORES_GREATER_THAN_74_KEY),
        projectionOperation);

    return mongoTemplate.aggregate(aggregation, "scorecardStats", ScorecardIdentifierAndScore.class).getMappedResults();
  }

  private StatsMetadata buildMetadata(BackstageCatalogEntity backstageCatalog) {
    return StatsMetadata.builder()
        .kind(backstageCatalog.getKind())
        .namespace(backstageCatalog.getMetadata().getNamespace())
        .name(backstageCatalog.getMetadata().getName())
        .type(BackstageCatalogEntityTypes.getEntityType(backstageCatalog))
        .owner(BackstageCatalogEntityTypes.getEntityOwner(backstageCatalog))
        .system(BackstageCatalogEntityTypes.getEntitySystem(backstageCatalog))
        .build();
  }
}
