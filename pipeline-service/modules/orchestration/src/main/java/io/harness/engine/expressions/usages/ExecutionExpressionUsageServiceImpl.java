/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.engine.expressions.usages;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.expressions.usages.beans.ExecutionExpressionUsagesEntity;
import io.harness.engine.expressions.usages.beans.ExecutionExpressionUsagesEntity.ExecutionExpressionUsagesEntityKeys;
import io.harness.mongo.helper.AnalyticsMongoTemplateHolder;

import com.google.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
public class ExecutionExpressionUsageServiceImpl implements ExecutionExpressionUsageService {
  private final MongoTemplate mongoTemplate;
  private final MongoTemplate secondaryMongoTemplate;

  @Inject
  public ExecutionExpressionUsageServiceImpl(
      MongoTemplate mongoTemplate, AnalyticsMongoTemplateHolder analyticsMongoTemplateHolder) {
    this.mongoTemplate = mongoTemplate;
    this.secondaryMongoTemplate = analyticsMongoTemplateHolder.getAnalyticsMongoTemplate();
  }

  @Override
  public void saveExpressions(List<ExecutionExpressionUsagesEntity> expressions) {
    mongoTemplate.insertAll(expressions);
  }

  @Override
  public List<ExecutionExpressionUsagesEntity> getExpressions(String nodeExecutionId) {
    if (EmptyPredicate.isEmpty(nodeExecutionId)) {
      return new LinkedList<>();
    }
    Query query = query(where(ExecutionExpressionUsagesEntityKeys.nodeExecutionId).is(nodeExecutionId));
    return secondaryMongoTemplate.find(query, ExecutionExpressionUsagesEntity.class);
  }
}
