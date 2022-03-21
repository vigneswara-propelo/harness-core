/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories.envGroup;

import static io.harness.exception.WingsException.USER_SRE;

import static java.lang.String.format;

import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.cdng.events.EnvironmentGroupCreateEvent;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxService;

import com.google.inject.Inject;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.query.Criteria;
/*
   TODO: need to do changes for environment group feature based on if this is available to or will be behind enforcement
   plan
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class EnvironmentGroupRepositoryCustomImpl implements EnvironmentGroupRepositoryCustom {
  private final GitSyncSdkService gitSyncSdkService;
  private final GitAwarePersistence gitAwarePersistence;
  private final OutboxService outboxService;

  @Override
  public Optional<EnvironmentGroupEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String envGroupId, boolean notDeleted) {
    return gitAwarePersistence.findOne(Criteria.where(EnvironmentGroupEntity.EnvironmentGroupKeys.deleted)
                                           .is(!notDeleted)
                                           .and(EnvironmentGroupEntity.EnvironmentGroupKeys.projectIdentifier)
                                           .is(projectIdentifier)
                                           .and(EnvironmentGroupEntity.EnvironmentGroupKeys.orgIdentifier)
                                           .is(orgIdentifier)
                                           .and(EnvironmentGroupEntity.EnvironmentGroupKeys.accountId)
                                           .is(accountId)
                                           .and(EnvironmentGroupEntity.EnvironmentGroupKeys.identifier)
                                           .is(envGroupId),
        projectIdentifier, orgIdentifier, accountId, EnvironmentGroupEntity.class);
  }

  @Override
  public EnvironmentGroupEntity create(EnvironmentGroupEntity environmentGroupEntity) {
    EnvironmentGroupEntity savedEntity;
    Supplier<OutboxEvent> functor = null;
    // If git sync is disabled, then outbox the event
    boolean gitSyncEnabled = gitSyncSdkService.isGitSyncEnabled(environmentGroupEntity.getAccountIdentifier(),
        environmentGroupEntity.getOrgIdentifier(), environmentGroupEntity.getProjectIdentifier());
    if (!gitSyncEnabled) {
      functor = ()
          -> outboxService.save(EnvironmentGroupCreateEvent.builder()
                                    .accountIdentifier(environmentGroupEntity.getAccountIdentifier())
                                    .orgIdentifier(environmentGroupEntity.getOrgIdentifier())
                                    .projectIdentifier(environmentGroupEntity.getProjectIdentifier())
                                    .environmentGroupEntity(environmentGroupEntity)
                                    .build());
    }
    try {
      savedEntity = gitAwarePersistence.save(environmentGroupEntity, environmentGroupEntity.getYaml(), ChangeType.ADD,
          EnvironmentGroupEntity.class, functor);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          format("Identifier %s already exists in organization id %s and project id %s",
              environmentGroupEntity.getIdentifier(), environmentGroupEntity.getProjectIdentifier(),
              environmentGroupEntity.getOrgIdentifier()),
          USER_SRE, ex);
    } catch (Exception e) {
      throw new InvalidRequestException(
          String.format("Error occurred while saving environment group - %s", e.getMessage()));
    }

    return savedEntity;
  }
}
