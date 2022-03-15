/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories.envGroup;

import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.gitsync.persistance.GitAwarePersistence;

import com.google.inject.Inject;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class EnvironmentGroupRepositoryCustomImpl implements EnvironmentGroupRepositoryCustom {
  private final GitAwarePersistence gitAwarePersistence;

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
}
