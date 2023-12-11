/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.SSCA;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ssca.entities.BaselineEntity;
import io.harness.ssca.entities.BaselineEntity.BaselineEntityKeys;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(SSCA)
@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
public class BaselineRepositoryCustomImpl implements BaselineRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public void upsert(BaselineEntity baselineEntity) {
    updateTagAndOrchestrationId(baselineEntity);
  }

  @Override
  public BaselineEntity findOne(String accountId, String orgId, String projectId, String artifactId) {
    Criteria criteria = Criteria.where(BaselineEntityKeys.accountIdentifier)
                            .is(accountId)
                            .and(BaselineEntityKeys.orgIdentifier)
                            .is(orgId)
                            .and(BaselineEntityKeys.projectIdentifier)
                            .is(projectId)
                            .and(BaselineEntityKeys.artifactId)
                            .is(artifactId);

    Query query = new Query(criteria);

    return mongoTemplate.findOne(query, BaselineEntity.class);
  }

  public BaselineEntity updateTagAndOrchestrationId(BaselineEntity baselineEntity) {
    Criteria criteria = Criteria.where(BaselineEntityKeys.accountIdentifier)
                            .is(baselineEntity.getAccountIdentifier())
                            .and(BaselineEntityKeys.orgIdentifier)
                            .is(baselineEntity.getOrgIdentifier())
                            .and(BaselineEntityKeys.projectIdentifier)
                            .is(baselineEntity.getProjectIdentifier())
                            .and(BaselineEntityKeys.artifactId)
                            .is(baselineEntity.getArtifactId());

    Query query = new Query(criteria);
    Update update = new Update();
    update.set(BaselineEntityKeys.tag, baselineEntity.getTag());
    update.set(BaselineEntityKeys.orchestrationId, baselineEntity.getOrchestrationId());
    FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true).upsert(true);
    return mongoTemplate.findAndModify(query, update, options, BaselineEntity.class);
  }

  public List<BaselineEntity> findAll(String accountId, String orgId, String projectId, List<String> orchestrationIds) {
    Criteria criteria = Criteria.where(BaselineEntityKeys.accountIdentifier)
                            .is(accountId)
                            .and(BaselineEntityKeys.orgIdentifier)
                            .is(orgId)
                            .and(BaselineEntityKeys.projectIdentifier)
                            .is(projectId)
                            .and(BaselineEntityKeys.orchestrationId)
                            .in(orchestrationIds);

    Query query = new Query(criteria);
    return mongoTemplate.find(query, BaselineEntity.class);
  }

  public void updateOrchestrationId(
      String accountId, String orgId, String projectId, String artifactId, String tag, String orchestrationId) {
    Criteria criteria = Criteria.where(BaselineEntityKeys.accountIdentifier)
                            .is(accountId)
                            .and(BaselineEntityKeys.orgIdentifier)
                            .is(orgId)
                            .and(BaselineEntityKeys.projectIdentifier)
                            .is(projectId)
                            .and(BaselineEntityKeys.artifactId)
                            .is(artifactId)
                            .and(BaselineEntityKeys.tag)
                            .is(tag);

    Query query = new Query(criteria);
    Update update = new Update();
    update.set(BaselineEntityKeys.orchestrationId, orchestrationId);
    FindAndModifyOptions options = new FindAndModifyOptions();
    mongoTemplate.findAndModify(query, update, options, BaselineEntity.class);
  }
}
