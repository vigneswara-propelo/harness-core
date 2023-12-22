/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.views.dao;

import static io.harness.beans.FeatureName.CCM_ENABLE_AZURE_CLOUD_ASSET_GOVERNANCE_UI;
import static io.harness.ccm.views.helper.RuleCostType.REALIZED;
import static io.harness.ccm.views.helper.RuleExecutionStatusType.SUCCESS;
import static io.harness.persistence.HQuery.excludeValidate;
import static io.harness.timescaledb.Tables.CE_RECOMMENDATIONS;

import static dev.morphia.aggregation.Accumulator.accumulator;
import static dev.morphia.aggregation.Group.grouping;
import static dev.morphia.aggregation.Projection.expression;
import static dev.morphia.aggregation.Projection.projection;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;

import io.harness.annotations.retry.RetryOnException;
import io.harness.ccm.commons.beans.recommendation.CCMJiraDetails;
import io.harness.ccm.commons.beans.recommendation.CCMServiceNowDetails;
import io.harness.ccm.commons.beans.recommendation.RecommendationState;
import io.harness.ccm.commons.beans.recommendation.ResourceType;
import io.harness.ccm.commons.entities.CCMSortOrder;
import io.harness.ccm.commons.entities.CCMTimeFilter;
import io.harness.ccm.commons.entities.recommendations.RecommendationGovernanceRuleId;
import io.harness.ccm.views.entities.RuleExecution;
import io.harness.ccm.views.entities.RuleExecution.RuleExecutionKeys;
import io.harness.ccm.views.entities.RuleExecutionSortType;
import io.harness.ccm.views.entities.RuleRecommendation;
import io.harness.ccm.views.entities.RuleRecommendation.RuleRecommendationId;
import io.harness.ccm.views.helper.GovernanceRuleFilter;
import io.harness.ccm.views.helper.OverviewExecutionDetails;
import io.harness.ccm.views.helper.RuleCloudProviderType;
import io.harness.ccm.views.helper.RuleExecutionFilter;
import io.harness.ccm.views.helper.RuleExecutionList;
import io.harness.ccm.views.helper.RuleExecutionStatusType;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.AggregationOptions;
import dev.morphia.aggregation.Projection;
import dev.morphia.query.CriteriaContainer;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@Singleton
public class RuleExecutionDAO {
  private static final String COUNT = "count";
  private static final String MONGO_ID = "_id";
  private static final String NULL_STRING = "null";
  private static final String SUM_OPERATION = "$sum";
  private static final String FORMATTED_DATE = "formattedDate";
  private static final String DATE_TO_STRING_OPERATION = "$dateToString";
  private static final String FORMAT_OPTION = "format";
  private static final String DATE_FORMAT = "%Y-%m-%d";
  private static final String DATE_OPTION = "date";
  private static final String TO_DATE_OPERATION = "$toDate";
  private static final String CREATED_AT_FIELD = "$createdAt";

  @Inject private HPersistence hPersistence;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private RuleDAO ruleDAO;
  @Inject private RuleEnforcementDAO ruleEnforcementDAO;
  @Inject private DSLContext dslContext;
  @Inject private FeatureFlagService featureFlagService;

  private static final int RETRY_COUNT = 3;
  private static final int SLEEP_DURATION = 100;

  public String save(RuleExecution ruleExecution) {
    return hPersistence.save(ruleExecution);
  }

  public List<String> save(List<RuleExecution> ruleExecutions) {
    return hPersistence.save(ruleExecutions);
  }

  public List<RuleExecution> list(String accountId) {
    return hPersistence.createQuery(RuleExecution.class).field(RuleExecutionKeys.accountId).equal(accountId).asList();
  }
  public RuleExecution get(String accountId, String uuid) {
    Query<RuleExecution> query = hPersistence.createQuery(RuleExecution.class, excludeValidate);
    query.field(RuleExecutionKeys.accountId).equal(accountId).field(RuleExecutionKeys.uuid).equal(uuid);
    return query.get();
  }
  public RuleExecutionList filterExecutionInternal(RuleExecutionFilter ruleExecutionFilter) {
    RuleExecutionList ruleExecutionList = RuleExecutionList.builder().build();
    Query<RuleExecution> query = hPersistence.createQuery(RuleExecution.class)
                                     .field(RuleExecutionKeys.accountId)
                                     .equal(ruleExecutionFilter.getAccountId());
    if (ruleExecutionFilter.getExecutionIds() != null) {
      query.field(RuleExecutionKeys.uuid).in(ruleExecutionFilter.getExecutionIds());
    }
    ruleExecutionList.setTotalItems(query.asList().size());
    ruleExecutionList.setRuleExecution(query.limit(ruleExecutionFilter.getLimit())
                                           .offset(ruleExecutionFilter.getOffset())
                                           .order(Sort.descending(RuleExecutionKeys.lastUpdatedAt))
                                           .asList());

    return ruleExecutionList;
  }
  public RuleExecutionList filterExecution(RuleExecutionFilter ruleExecutionFilter) {
    Query<RuleExecution> query = getRuleExecutionFilterQuery(ruleExecutionFilter, RuleExecutionKeys.lastUpdatedAt);
    List<RuleExecution> ruleExecutions = query.limit(ruleExecutionFilter.getLimit())
                                             .offset(ruleExecutionFilter.getOffset())
                                             .order(getRuleExecutionSortSort(ruleExecutionFilter))
                                             .asList();
    return RuleExecutionList.builder().totalItems((int) query.count()).ruleExecution(ruleExecutions).build();
  }

  @NotNull
  private static Sort getRuleExecutionSortSort(RuleExecutionFilter ruleExecutionFilter) {
    final RuleExecutionSortType modifiedSortType = Objects.isNull(ruleExecutionFilter.getRuleExecutionSortType())
        ? RuleExecutionSortType.COST
        : ruleExecutionFilter.getRuleExecutionSortType();
    return (Objects.isNull(ruleExecutionFilter.getSortOrder())
               || ruleExecutionFilter.getSortOrder() == CCMSortOrder.DESCENDING)
        ? Sort.descending(modifiedSortType.getColumnName())
        : Sort.ascending(modifiedSortType.getColumnName());
  }

  @NotNull
  private Query<RuleExecution> getRuleExecutionFilterQuery(RuleExecutionFilter ruleExecutionFilter, String dateField) {
    Query<RuleExecution> query = hPersistence.createQuery(RuleExecution.class);
    CriteriaContainer criteria = query.or(query.criteria(RuleExecutionKeys.executionType).notEqual("INTERNAL"),
        query.criteria(RuleExecutionKeys.executionType).doesNotExist());
    query.and(criteria, query.criteria(RuleExecutionKeys.accountId).equal(ruleExecutionFilter.getAccountId()));
    if (ruleExecutionFilter.getSavings() != null) {
      CriteriaContainer criteriaSort =
          query.criteria(RuleExecutionKeys.cost).greaterThanOrEq(ruleExecutionFilter.getSavings());
      query.and(criteriaSort);
    }
    if (ruleExecutionFilter.getTargetAccount() != null) {
      query.field(RuleExecutionKeys.targetAccount).in(ruleExecutionFilter.getTargetAccount());
    }
    if (ruleExecutionFilter.getRuleIds() != null) {
      query.field(RuleExecutionKeys.ruleIdentifier).in(ruleExecutionFilter.getRuleIds());
    }
    if (ruleExecutionFilter.getExecutionIds() != null) {
      query.field(RuleExecutionKeys.uuid).in(ruleExecutionFilter.getExecutionIds());
    }
    if (ruleExecutionFilter.getRuleSetIds() != null) {
      query.field(RuleExecutionKeys.rulePackIdentifier).in(ruleExecutionFilter.getRuleSetIds());
    }
    if (ruleExecutionFilter.getRuleEnforcementId() != null) {
      query.field(RuleExecutionKeys.ruleEnforcementIdentifier).in(ruleExecutionFilter.getRuleEnforcementId());
    }
    if (ruleExecutionFilter.getRegion() != null) {
      query.field(RuleExecutionKeys.targetRegions).in(ruleExecutionFilter.getRegion());
    }
    if (ruleExecutionFilter.getExecutionStatus() != null) {
      query.field(RuleExecutionKeys.executionStatus).equal(ruleExecutionFilter.getExecutionStatus());
    }
    if (ruleExecutionFilter.getCloudProvider() != null) {
      query.field(RuleExecutionKeys.cloudProvider).equal(ruleExecutionFilter.getCloudProvider());
    }
    if (ruleExecutionFilter.getTime() != null) {
      for (CCMTimeFilter time : ruleExecutionFilter.getTime()) {
        switch (time.getOperator()) {
          case AFTER:
            query.field(dateField).greaterThanOrEq(time.getTimestamp());
            break;
          case BEFORE:
            query.field(dateField).lessThanOrEq(time.getTimestamp());
            break;
          default:
            throw new InvalidRequestException("Operator not supported not supported for time fields");
        }
      }
    }
    if (featureFlagService.isNotEnabled(
            CCM_ENABLE_AZURE_CLOUD_ASSET_GOVERNANCE_UI, ruleExecutionFilter.getAccountId())) {
      query.field(RuleExecutionKeys.cloudProvider).notEqual(RuleCloudProviderType.AZURE);
    }
    return query;
  }

  public Map<String, Integer> getResourceTypeCountMapping(RuleExecutionFilter ruleExecutionFilter) {
    Map<String, Integer> resourceTypeCountMap = new HashMap<>();
    Query<RuleExecution> query = getRuleExecutionFilterQuery(ruleExecutionFilter, RuleExecutionKeys.lastUpdatedAt);
    query.filter(RuleExecutionKeys.executionStatus, SUCCESS);
    // noinspection rawtypes
    Iterator<Map> aggregationResult =
        hPersistence.getDatastore(RuleExecution.class)
            .createAggregation(RuleExecution.class)
            .match(query)
            .group(RuleExecutionKeys.resourceType, grouping(COUNT, accumulator(SUM_OPERATION, 1)))
            .sort(Sort.descending(COUNT))
            .aggregate(Map.class, AggregationOptions.builder().build());
    while (aggregationResult.hasNext()) {
      // noinspection rawtypes
      Map resultEntry = aggregationResult.next();
      resourceTypeCountMap.put((String) resultEntry.get(MONGO_ID), (int) resultEntry.get(COUNT));
    }
    if (resourceTypeCountMap.containsKey(null)) {
      Integer value = resourceTypeCountMap.remove(null);
      resourceTypeCountMap.put(NULL_STRING, value);
    }
    return resourceTypeCountMap;
  }

  public Map<String, Double> getRealisedSavings(RuleExecutionFilter ruleExecutionFilter) {
    Map<String, Double> realisedSavingsMap = new HashMap<>();
    Query<RuleExecution> query = getRuleExecutionFilterQuery(ruleExecutionFilter, RuleExecutionKeys.createdAt);
    query.field(RuleExecutionKeys.cost).exists().filter(RuleExecutionKeys.costType, REALIZED);
    Projection formattedDateProjection = expression(FORMATTED_DATE,
        new Document(DATE_TO_STRING_OPERATION,
            new Document(FORMAT_OPTION, DATE_FORMAT)
                .append(DATE_OPTION, new Document(TO_DATE_OPERATION, CREATED_AT_FIELD))));
    // noinspection rawtypes
    Iterator<Map> aggregationResult =
        hPersistence.getDatastore(RuleExecution.class)
            .createAggregation(RuleExecution.class)
            .match(query)
            .project(formattedDateProjection, projection(RuleExecutionKeys.cost))
            .group(FORMATTED_DATE, grouping(RuleExecutionKeys.cost, accumulator(SUM_OPERATION, RuleExecutionKeys.cost)))
            .aggregate(Map.class, AggregationOptions.builder().build());
    while (aggregationResult.hasNext()) {
      // noinspection rawtypes
      Map resultEntry = aggregationResult.next();
      realisedSavingsMap.put(String.valueOf(resultEntry.get(MONGO_ID)),
          Double.valueOf(String.valueOf(resultEntry.get(RuleExecutionKeys.cost))));
    }
    return realisedSavingsMap;
  }

  public List<RuleExecution> getRuleLastExecution(String accountId, List<String> ruleIds) {
    Criteria criteria = Criteria.where(RuleExecutionKeys.accountId)
                            .is(accountId)
                            .and(RuleExecutionKeys.ruleIdentifier)
                            .in(ruleIds)
                            .and(RuleExecutionKeys.executionStatus)
                            .in(List.of(RuleExecutionStatusType.FAILED.name(), RuleExecutionStatusType.SUCCESS.name()))
                            .orOperator(Criteria.where(RuleExecutionKeys.executionType).ne("INTERNAL"),
                                Criteria.where(RuleExecutionKeys.executionType).is(null));
    if (featureFlagService.isNotEnabled(CCM_ENABLE_AZURE_CLOUD_ASSET_GOVERNANCE_UI, accountId)) {
      criteria.and(RuleExecutionKeys.cloudProvider).ne(RuleCloudProviderType.AZURE);
    }

    GroupOperation group = group(RuleExecutionKeys.ruleIdentifier).first("$$ROOT").as("lastExecution");
    MatchOperation matchStage = Aggregation.match(criteria);
    SortOperation sortStage = sort(org.springframework.data.domain.Sort.by(
        org.springframework.data.domain.Sort.Direction.DESC, RuleExecutionKeys.lastUpdatedAt));

    return mongoTemplate
        .aggregate(Aggregation.newAggregation(matchStage, sortStage, group, Aggregation.replaceRoot("$lastExecution")),
            "governanceRuleExecution", RuleExecution.class)
        .getMappedResults();
  }

  public OverviewExecutionDetails getOverviewExecutionDetails(String accountId) {
    OverviewExecutionDetails overviewExecutionDetails = OverviewExecutionDetails.builder().build();
    overviewExecutionDetails.setTotalRules(
        ruleDAO.list(GovernanceRuleFilter.builder().accountId(accountId).build()).getRules().size());
    overviewExecutionDetails.setTotalRuleEnforcements(ruleEnforcementDAO.list(accountId).size());
    return overviewExecutionDetails;
  }

  public void updateJiraInGovernanceRecommendation(
      @NonNull String accountId, @NonNull String id, CCMJiraDetails jiraDetails) {
    hPersistence.upsert(hPersistence.createQuery(RuleRecommendation.class)
                            .filter(RuleRecommendationId.accountId, accountId)
                            .filter(RuleRecommendationId.uuid, new ObjectId(id)),
        hPersistence.createUpdateOperations(RuleRecommendation.class)
            .set(RuleRecommendationId.jiraDetails, jiraDetails));
  }

  public void updateServicenowDetailsInGovernanceRecommendation(
      @NonNull String accountId, @NonNull String id, CCMServiceNowDetails serviceNowDetails) {
    hPersistence.upsert(hPersistence.createQuery(RuleRecommendation.class)
                            .filter(RuleRecommendationId.accountId, accountId)
                            .filter(RuleRecommendationId.uuid, new ObjectId(id)),
        hPersistence.createUpdateOperations(RuleRecommendation.class)
            .set(RuleRecommendationId.serviceNowDetails, serviceNowDetails));
  }

  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public void ignoreGovernanceRecommendations(
      @NonNull String accountId, @NonNull List<RecommendationGovernanceRuleId> governanceRuleIds) {
    if (governanceRuleIds.isEmpty()) {
      return;
    }
    dslContext.update(CE_RECOMMENDATIONS)
        .set(CE_RECOMMENDATIONS.RECOMMENDATIONSTATE, RecommendationState.IGNORED.name())
        .where(CE_RECOMMENDATIONS.ACCOUNTID.eq(accountId)
                   .and(CE_RECOMMENDATIONS.RECOMMENDATIONSTATE.eq(RecommendationState.OPEN.name()))
                   .and(CE_RECOMMENDATIONS.RESOURCETYPE.eq(ResourceType.GOVERNANCE.name()))
                   .and(getGovernanceCondition(governanceRuleIds)))
        .execute();
  }

  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public void unIgnoreGovernanceRecommendations(
      @NonNull String accountId, @NonNull List<RecommendationGovernanceRuleId> governanceRuleIds) {
    if (governanceRuleIds.isEmpty()) {
      return;
    }
    dslContext.update(CE_RECOMMENDATIONS)
        .set(CE_RECOMMENDATIONS.RECOMMENDATIONSTATE, RecommendationState.OPEN.name())
        .where(CE_RECOMMENDATIONS.ACCOUNTID.eq(accountId)
                   .and(CE_RECOMMENDATIONS.RECOMMENDATIONSTATE.eq(RecommendationState.IGNORED.name()))
                   .and(CE_RECOMMENDATIONS.RESOURCETYPE.eq(ResourceType.GOVERNANCE.name()))
                   .and(getGovernanceCondition(governanceRuleIds)))
        .execute();
  }

  private Condition getGovernanceCondition(List<RecommendationGovernanceRuleId> governanceRuleIds) {
    RecommendationGovernanceRuleId governanceRuleId = governanceRuleIds.get(0);
    Condition condition = CE_RECOMMENDATIONS.GOVERNANCERULEID.eq(governanceRuleId.getRuleId());
    for (int i = 1; i < governanceRuleIds.size(); i++) {
      governanceRuleId = governanceRuleIds.get(i);
      condition.or(CE_RECOMMENDATIONS.GOVERNANCERULEID.eq(governanceRuleId.getRuleId()));
    }
    return condition;
  }

  @NonNull
  public List<RuleRecommendation> getGovernanceRecommendations(
      @NonNull String accountId, @NonNull List<String> recommendationIds) {
    List<ObjectId> recommendationObjectIds = recommendationIds.stream().map(ObjectId::new).collect(Collectors.toList());
    return hPersistence.createQuery(RuleRecommendation.class)
        .project(RuleRecommendationId.executions, true)
        .field(RuleRecommendationId.accountId)
        .equal(accountId)
        .field(RuleRecommendationId.uuid)
        .in(recommendationObjectIds)
        .asList();
  }

  public long countRuleRecommendations(String accountId) {
    return hPersistence.createQuery(RuleRecommendation.class)
        .field(RuleRecommendationId.accountId)
        .equal(accountId)
        .count();
  }

  public boolean deleteAllRuleRecommendationsForAccount(String accountId) {
    Query<RuleRecommendation> query =
        hPersistence.createQuery(RuleRecommendation.class).field(RuleRecommendationId.accountId).equal(accountId);
    return hPersistence.delete(query);
  }

  public long countRuleExecutions(String accountId) {
    return hPersistence.createQuery(RuleExecution.class).field(RuleExecutionKeys.accountId).equal(accountId).count();
  }

  public boolean deleteAllRuleExecutionsForAccount(String accountId) {
    Query<RuleExecution> query =
        hPersistence.createQuery(RuleExecution.class).field(RuleExecutionKeys.accountId).equal(accountId);
    return hPersistence.delete(query);
  }
}
