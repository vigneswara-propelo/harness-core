/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.SSCA;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ssca.entities.ConfigEntity;
import io.harness.ssca.entities.ConfigEntity.ConfigEntityKeys;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@OwnedBy(SSCA)
@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
public class ConfigRepoCustomImpl implements ConfigRepoCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public ConfigEntity saveOrUpdate(ConfigEntity configEntity) {
    ConfigEntity entity = findOne(
        configEntity.getAccountId(), configEntity.getOrgId(), configEntity.getProjectId(), configEntity.getConfigId());

    if (entity == null) {
      return mongoTemplate.save(configEntity);
    }
    return update(configEntity, configEntity.getConfigId());
  }

  @Override
  public ConfigEntity update(ConfigEntity configEntity, String configId) {
    Criteria criteria = Criteria.where(ConfigEntityKeys.accountId)
                            .is(configEntity.getAccountId())
                            .and(ConfigEntityKeys.orgId)
                            .is(configEntity.getOrgId())
                            .and(ConfigEntityKeys.projectId)
                            .is(configEntity.getProjectId())
                            .and(ConfigEntityKeys.configId)
                            .is(configId);

    Query query = new Query(criteria);
    Update update = new Update();
    update.set(ConfigEntityKeys.configId, configEntity.getConfigId());
    update.set(ConfigEntityKeys.name, configEntity.getName());
    update.set(ConfigEntityKeys.type, configEntity.getType());
    update.set(ConfigEntityKeys.configInfos, configEntity.getConfigInfos());
    FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true);
    return mongoTemplate.findAndModify(query, update, options, ConfigEntity.class);
  }

  @Override
  public DeleteResult delete(String accountId, String orgId, String projectId, String configId) {
    Criteria criteria = Criteria.where(ConfigEntityKeys.accountId)
                            .is(accountId)
                            .and(ConfigEntityKeys.orgId)
                            .is(orgId)
                            .and(ConfigEntityKeys.projectId)
                            .is(projectId)
                            .and(ConfigEntityKeys.configId)
                            .is(configId);

    Query query = new Query(criteria);

    return mongoTemplate.remove(query, ConfigEntity.class);
  }

  @Override
  public ConfigEntity findOne(String accountId, String orgId, String projectId, String configId) {
    Criteria criteria = Criteria.where(ConfigEntityKeys.accountId)
                            .is(accountId)
                            .and(ConfigEntityKeys.orgId)
                            .is(orgId)
                            .and(ConfigEntityKeys.projectId)
                            .is(projectId)
                            .and(ConfigEntityKeys.configId)
                            .is(configId);

    Query query = new Query(criteria);

    return mongoTemplate.findOne(query, ConfigEntity.class);
  }

  @Override
  public ConfigEntity findByAccountIdAndProjectIdAndOrgIdAndNameAndType(
      String accountId, String orgId, String projectId, String name, String type) {
    if (isEmpty(name)) {
      throw new InvalidRequestException("Name of config should not be null or empty");
    }
    if (isEmpty(type)) {
      throw new InvalidRequestException("Type of config should not be null or empty");
    }
    Criteria criteria = Criteria.where(ConfigEntityKeys.accountId)
                            .is(accountId)
                            .and(ConfigEntityKeys.orgId)
                            .is(orgId)
                            .and(ConfigEntityKeys.projectId)
                            .is(projectId)
                            .and(ConfigEntityKeys.name)
                            .is(name)
                            .and(ConfigEntityKeys.type)
                            .is(type);

    Query query = new Query(criteria);

    return mongoTemplate.findOne(query, ConfigEntity.class);
  }

  @Override
  public Page<ConfigEntity> findAll(String accountId, String orgId, String projectId, Pageable pageable) {
    Criteria criteria = Criteria.where(ConfigEntityKeys.accountId)
                            .is(accountId)
                            .and(ConfigEntityKeys.orgId)
                            .is(orgId)
                            .and(ConfigEntityKeys.projectId)
                            .is(projectId);

    Query query = new Query(criteria).with(pageable);

    List<ConfigEntity> configEntities = mongoTemplate.find(query, ConfigEntity.class);
    return PageableExecutionUtils.getPage(
        configEntities, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), ConfigEntity.class));
  }
}
