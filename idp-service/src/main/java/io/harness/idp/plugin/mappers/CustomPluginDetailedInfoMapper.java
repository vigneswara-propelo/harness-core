/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.plugin.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.plugin.entities.CustomPluginInfoEntity;
import io.harness.spec.server.idp.v1.model.AppConfig;
import io.harness.spec.server.idp.v1.model.BackstageEnvSecretVariable;
import io.harness.spec.server.idp.v1.model.CustomPluginDetailedInfo;
import io.harness.spec.server.idp.v1.model.PluginInfo;
import io.harness.spec.server.idp.v1.model.ProxyHostDetail;

import java.util.List;

@OwnedBy(HarnessTeam.IDP)
public class CustomPluginDetailedInfoMapper
    implements PluginDetailedInfoMapper<CustomPluginDetailedInfo, CustomPluginInfoEntity> {
  @Override
  public CustomPluginDetailedInfo toDto(CustomPluginInfoEntity entity, AppConfig appConfig,
      List<BackstageEnvSecretVariable> secrets, List<ProxyHostDetail> hostDetails) {
    CustomPluginDetailedInfo dto = new CustomPluginDetailedInfo();
    setCommonFieldsDto(entity, dto, appConfig, secrets, hostDetails);
    dto.setArtifact(entity.getArtifact());
    return dto;
  }

  @Override
  public CustomPluginInfoEntity fromDto(CustomPluginDetailedInfo dto, String accountIdentifier) {
    CustomPluginInfoEntity entity = CustomPluginInfoEntity.builder().build();
    setCommonFieldsEntity(dto, entity, accountIdentifier);
    entity.setType(PluginInfo.PluginTypeEnum.CUSTOM);
    entity.setArtifact(dto.getArtifact());
    return entity;
  }
}
