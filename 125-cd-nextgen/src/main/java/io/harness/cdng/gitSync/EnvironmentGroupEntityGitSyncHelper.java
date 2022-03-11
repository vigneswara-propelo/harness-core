/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.gitSync;

import io.harness.EntityType;
import io.harness.cdng.envGroup.beans.EnvironmentGroupConfig;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity.EnvironmentGroupKeys;
import io.harness.common.EntityReference;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.FileChange;
import io.harness.gitsync.FullSyncChangeSet;
import io.harness.gitsync.ScopeDetails;
import io.harness.gitsync.entityInfo.AbstractGitSdkEntityHandler;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.ng.core.EntityDetail;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class EnvironmentGroupEntityGitSyncHelper
    extends AbstractGitSdkEntityHandler<EnvironmentGroupEntity, EnvironmentGroupConfig> {
  @Override
  public Optional<EntityGitDetails> getEntityDetailsIfExists(String accountIdentifier, String yaml) {
    return Optional.empty();
  }

  @Override
  public EnvironmentGroupConfig getYamlDTO(String yaml) {
    return null;
  }

  @Override
  public String getYamlFromEntityRef(EntityDetailProtoDTO entityReference) {
    return null;
  }

  @Override
  protected EnvironmentGroupConfig updateEntityFilePath(String accountIdentifier, String yaml, String newFilePath) {
    return null;
  }

  @Override
  public Supplier<EnvironmentGroupConfig> getYamlFromEntity(EnvironmentGroupEntity entity) {
    return null;
  }

  @Override
  public EntityType getEntityType() {
    return null;
  }

  @Override
  public Supplier<EnvironmentGroupEntity> getEntityFromYaml(EnvironmentGroupConfig yaml, String accountIdentifier) {
    return null;
  }

  @Override
  public EntityDetail getEntityDetail(EnvironmentGroupEntity entity) {
    return null;
  }

  @Override
  public EnvironmentGroupConfig save(String accountIdentifier, String yaml) {
    return null;
  }

  @Override
  public EnvironmentGroupConfig update(String accountIdentifier, String yaml, ChangeType changeType) {
    return null;
  }

  @Override
  public boolean markEntityInvalid(String accountIdentifier, EntityReference entityReference, String erroneousYaml) {
    return false;
  }

  @Override
  public boolean delete(EntityReference entityReference) {
    return false;
  }

  @Override
  public String getObjectIdOfYamlKey() {
    return EnvironmentGroupKeys.objectIdOfYaml;
  }

  @Override
  public String getIsFromDefaultBranchKey() {
    return EnvironmentGroupKeys.isFromDefaultBranch;
  }

  @Override
  public String getYamlGitConfigRefKey() {
    return EnvironmentGroupKeys.yamlGitConfigRef;
  }

  @Override
  public String getUuidKey() {
    return EnvironmentGroupKeys.uuid;
  }

  @Override
  public String getBranchKey() {
    return EnvironmentGroupKeys.branch;
  }

  @Override
  public EnvironmentGroupConfig fullSyncEntity(FullSyncChangeSet fullSyncChangeSet) {
    return null;
  }

  @Override
  public List<FileChange> listAllEntities(ScopeDetails scopeDetails) {
    return null;
  }
}
