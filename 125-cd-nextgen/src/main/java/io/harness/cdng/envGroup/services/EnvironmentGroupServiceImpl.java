/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.envGroup.services;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.exception.InvalidRequestException;
import io.harness.repositories.envGroup.EnvironmentGroupRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class EnvironmentGroupServiceImpl implements EnvironmentGroupService {
  private final EnvironmentGroupRepository environmentRepository;

  @Inject
  public EnvironmentGroupServiceImpl(EnvironmentGroupRepository environmentRepository) {
    this.environmentRepository = environmentRepository;
  }

  @Override
  public Optional<EnvironmentGroupEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String envGroupId, boolean deleted) {
    return environmentRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
        accountId, orgIdentifier, projectIdentifier, envGroupId, !deleted);
  }

  @Override
  public EnvironmentGroupEntity create(EnvironmentGroupEntity entity) {
    return environmentRepository.create(entity);
  }

  @Override
  public Page<EnvironmentGroupEntity> list(
      Criteria criteria, Pageable pageRequest, String projectIdentifier, String orgIdentifier, String accountId) {
    return environmentRepository.list(criteria, pageRequest, projectIdentifier, orgIdentifier, accountId);
  }

  @Override
  public EnvironmentGroupEntity delete(
      String accountId, String orgIdentifier, String projectIdentifier, String envGroupId, Long version) {
    Optional<EnvironmentGroupEntity> envGroupEntity =
        get(accountId, orgIdentifier, projectIdentifier, envGroupId, false);
    if (!envGroupEntity.isPresent()) {
      throw new InvalidRequestException(
          format("Environment Group [%s] under Project[%s], Organization [%s] doesn't exist.", envGroupId,
              projectIdentifier, orgIdentifier));
    }
    EnvironmentGroupEntity existingEntity = envGroupEntity.get();
    if (version != null && !version.equals(existingEntity.getVersion())) {
      throw new InvalidRequestException(
          format("Environment Group [%s] under Project[%s], Organization [%s] is not on the correct version.",
              envGroupId, projectIdentifier, orgIdentifier));
    }
    EnvironmentGroupEntity entityWithDelete = existingEntity.withDeleted(true);
    try {
      EnvironmentGroupEntity deletedEntity = environmentRepository.deleteEnvGroup(entityWithDelete);

      if (deletedEntity.getDeleted()) {
        return deletedEntity;
      } else {
        throw new InvalidRequestException(
            format("Environment Group Set [%s] under Project[%s], Organization [%s] couldn't be deleted.", envGroupId,
                projectIdentifier, orgIdentifier));
      }
    } catch (Exception e) {
      log.error(String.format("Error while deleting Environment Group [%s]", envGroupId), e);
      throw new InvalidRequestException(
          String.format("Error while deleting input set [%s]: %s", envGroupId, e.getMessage()));
    }
  }

  @Override
  public EnvironmentGroupEntity update(EnvironmentGroupEntity requestedEntity) {
    String accountId = requestedEntity.getAccountId();
    String orgId = requestedEntity.getOrgIdentifier();
    String projectId = requestedEntity.getProjectIdentifier();
    String envGroupId = requestedEntity.getIdentifier();

    Optional<EnvironmentGroupEntity> optionalEnvGroupEntity = get(accountId, orgId, projectId, envGroupId, false);
    if (!optionalEnvGroupEntity.isPresent()) {
      throw new InvalidRequestException(
          String.format("Environment Group %s in project %s in organization %s is either deleted or was not created",
              envGroupId, projectId, orgId));
    }

    EnvironmentGroupEntity originalEntity = optionalEnvGroupEntity.get();
    if (originalEntity.getVersion() != null && !originalEntity.getVersion().equals(originalEntity.getVersion())) {
      throw new InvalidRequestException(format(
          "Environment Group [%s] under Project[%s], Organization [%s] is not on the correct version.",
          originalEntity.getIdentifier(), originalEntity.getProjectIdentifier(), originalEntity.getOrgIdentifier()));
    }

    EnvironmentGroupEntity updatedEntity = originalEntity.withName(requestedEntity.getName())
                                               .withDescription(requestedEntity.getDescription())
                                               .withLastModifiedAt(System.currentTimeMillis())
                                               .withColor(requestedEntity.getColor())
                                               .withEnvIdentifiers(requestedEntity.getEnvIdentifiers())
                                               .withTags(requestedEntity.getTags())
                                               .withYaml(requestedEntity.getYaml());
    return environmentRepository.update(updatedEntity, originalEntity);
  }
}
