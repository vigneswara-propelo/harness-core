/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.exception.InvalidRequestException;
import io.harness.repositories.ConfigRepo;
import io.harness.spec.server.ssca.v1.model.ConfigInfo;
import io.harness.spec.server.ssca.v1.model.ConfigInfoConfig;
import io.harness.spec.server.ssca.v1.model.ConfigRequestBody;
import io.harness.spec.server.ssca.v1.model.ConfigResponseBody;
import io.harness.ssca.entities.ConfigEntity;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Slf4j
public class ConfigServiceImpl implements ConfigService {
  @Inject ConfigRepo configRepo;
  @Override
  public void deleteConfigById(String orgId, String projectId, String configId, String accountId) {
    validatePathParamsWithConfigId(accountId, orgId, projectId, configId);

    ConfigEntity configEntity = configRepo.findOne(accountId, orgId, projectId, configId);
    DeleteResult deleteResult = configRepo.delete(accountId, orgId, projectId, configId);

    if (deleteResult.getDeletedCount() == 0 || configEntity == null) {
      log.error(String.format("Could not delete config for id [%s]", configId));
    }
  }

  @Override
  public ConfigResponseBody getConfigById(String orgId, String projectId, String configId, String accountId) {
    ConfigEntity configEntity = configRepo.findOne(accountId, orgId, projectId, configId);
    if (configEntity == null) {
      throw new NotFoundException(String.format("Config not found for id [%s]", configId));
    }
    return toConfigResponse(configEntity);
  }

  @Override
  public ConfigResponseBody getConfigByNameAndType(
      String orgId, String projectId, String name, String type, String accountId) {
    validatePathParams(accountId, orgId, projectId);
    if (isEmpty(name)) {
      throw new InvalidRequestException("Name of config should not be null or empty");
    }
    if (isEmpty(type)) {
      throw new InvalidRequestException("Type of config should not be null or empty");
    }

    ConfigEntity configEntity =
        configRepo.findByAccountIdAndProjectIdAndOrgIdAndNameAndType(accountId, orgId, projectId, name, type);
    if (configEntity == null) {
      throw new NotFoundException(String.format("Config not found for name [%s] and type [%s]", name, type));
    }
    return toConfigResponse(configEntity);
  }

  @Override
  public void saveConfig(String orgId, String projectId, ConfigRequestBody body, String accountId) {
    validateRequest(body);
    ConfigEntity configEntity = toConfigEntity(body);
    configRepo.saveOrUpdate(configEntity);
  }

  @Override
  public void updateConfigById(
      String orgId, String projectId, String configId, ConfigRequestBody body, String accountId) {
    validatePathParamsWithConfigId(accountId, orgId, projectId, configId);
    validateRequest(body);
    ConfigEntity configEntity = toConfigEntity(body);
    configRepo.update(configEntity, configId);
  }

  @Override
  public Page<ConfigResponseBody> listConfigs(String orgId, String projectId, String accountId, Pageable pageable) {
    validatePathParams(accountId, orgId, projectId);

    return configRepo.findAll(accountId, orgId, projectId, pageable)
        .map(configEntity -> toConfigResponse(configEntity));
  }

  public ConfigEntity toConfigEntity(ConfigRequestBody body) {
    return ConfigEntity.builder()
        .accountId(body.getAccountId())
        .orgId(body.getOrgId())
        .projectId(body.getProjectId())
        .configId(body.getConfigId())
        .userId(body.getUserId())
        .name(body.getName())
        .type(body.getType())
        .creationOn(body.getCreationOn())
        .configInfos(body.getConfigInfo()
                         .stream()
                         .map(configInfo
                             -> ConfigEntity.ConfigInfo.builder()
                                    .id(configInfo.getId())
                                    .categoryName(configInfo.getName())
                                    .config(configInfo.getConfig().stream().collect(
                                        Collectors.toMap(ConfigInfoConfig::getKey, ConfigInfoConfig::getValue)))
                                    .build())
                         .collect(Collectors.toList()))
        .build();
  }

  public ConfigResponseBody toConfigResponse(ConfigEntity configEntity) {
    return new ConfigResponseBody()
        .accountId(configEntity.getAccountId())
        .orgId(configEntity.getOrgId())
        .projectId(configEntity.getProjectId())
        .configId(configEntity.getConfigId())
        .userId(configEntity.getUserId())
        .name(configEntity.getName())
        .type(configEntity.getType())
        .creationOn(configEntity.getCreationOn())
        .configInfo(
            configEntity.getConfigInfos()
                .stream()
                .map(configInfo
                    -> new ConfigInfo()
                           .id(configInfo.getId())
                           .name(configInfo.getCategoryName())
                           .config(configInfo.getConfig()
                                       .entrySet()
                                       .stream()
                                       .map(entry -> new ConfigInfoConfig().key(entry.getKey()).value(entry.getValue()))
                                       .collect(Collectors.toList())))
                .collect(Collectors.toList()));
  }

  private void validatePathParams(String accountId, String orgId, String projectId) {
    if (isEmpty(accountId)) {
      throw new InvalidRequestException("Account Id should not be null or empty");
    }
    if (isEmpty(orgId)) {
      throw new InvalidRequestException("Org Id should not be null or empty");
    }
    if (isEmpty(projectId)) {
      throw new InvalidRequestException("Project Id should not be null or empty");
    }
  }

  private void validatePathParamsWithConfigId(String accountId, String orgId, String projectId, String configId) {
    validatePathParams(accountId, orgId, projectId);
    if (isEmpty(configId)) {
      throw new InvalidRequestException("Config Id should not be null or empty");
    }
  }

  private void validateRequest(ConfigRequestBody body) {
    if (isEmpty(body.getAccountId())) {
      throw new InvalidRequestException("Account Id should not be null or empty");
    }
    if (isEmpty(body.getOrgId())) {
      throw new InvalidRequestException("Org Id should not be null or empty");
    }
    if (isEmpty(body.getProjectId())) {
      throw new InvalidRequestException("Project Id should not be null or empty");
    }
    if (isEmpty(body.getConfigId())) {
      throw new InvalidRequestException("Config Id should not be null or empty");
    }
  }
}
