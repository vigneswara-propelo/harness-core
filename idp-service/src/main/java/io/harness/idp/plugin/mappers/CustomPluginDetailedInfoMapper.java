/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.plugin.mappers;

import static io.harness.spec.server.idp.v1.model.Artifact.TypeEnum.ZIP;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.plugin.beans.FileType;
import io.harness.idp.plugin.entities.CustomPluginInfoEntity;
import io.harness.idp.plugin.entities.PluginInfoEntity;
import io.harness.spec.server.idp.v1.model.AppConfig;
import io.harness.spec.server.idp.v1.model.Artifact;
import io.harness.spec.server.idp.v1.model.BackstageEnvSecretVariable;
import io.harness.spec.server.idp.v1.model.CustomPluginDetailedInfo;
import io.harness.spec.server.idp.v1.model.PluginInfo;
import io.harness.spec.server.idp.v1.model.ProxyHostDetail;

import java.util.ArrayList;
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

  public void addFileUploadDetails(PluginInfoEntity entity, String fileType, String gcsBucketUrl) {
    CustomPluginInfoEntity customPluginInfoEntity = (CustomPluginInfoEntity) entity;
    switch (FileType.valueOf(fileType)) {
      case ZIP:
        Artifact artifact = new Artifact();
        artifact.setType(ZIP);
        artifact.setUrl(gcsBucketUrl);
        customPluginInfoEntity.setArtifact(artifact);
        break;
      case ICON:
        customPluginInfoEntity.setIconUrl(gcsBucketUrl);
        break;
      case SCREENSHOT:
        List<String> images = customPluginInfoEntity.getImages();
        if (images == null) {
          images = new ArrayList<>();
        }
        images.add(gcsBucketUrl);
        customPluginInfoEntity.setImages(images);
        break;
      default:
        throw new UnsupportedOperationException(String.format("File type %s is not supported", fileType));
    }
  }

  public void removeFileDetails(PluginInfoEntity entity, String fileType, String gcsBucketUrl) {
    CustomPluginInfoEntity customPluginInfoEntity = (CustomPluginInfoEntity) entity;
    switch (FileType.valueOf(fileType)) {
      case ZIP:
        Artifact artifact = customPluginInfoEntity.getArtifact();
        if (artifact != null && ZIP.equals(artifact.getType()) && gcsBucketUrl.equals(artifact.getUrl())) {
          customPluginInfoEntity.setArtifact(null);
        }
        break;
      case ICON:
        if (gcsBucketUrl.equals(customPluginInfoEntity.getIconUrl())) {
          customPluginInfoEntity.setIconUrl(null);
        }
        break;
      case SCREENSHOT:
        List<String> images = customPluginInfoEntity.getImages();
        if (images != null && !images.isEmpty()) {
          images.remove(gcsBucketUrl);
        }
        customPluginInfoEntity.setImages(images);
        break;
      default:
        throw new UnsupportedOperationException(String.format("File type %s is not supported", fileType));
    }
  }
}
