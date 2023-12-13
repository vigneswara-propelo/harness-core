/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.plugin.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.plugin.beans.ExportsData;
import io.harness.idp.plugin.entities.PluginInfoEntity;
import io.harness.idp.plugin.enums.ExportType;
import io.harness.spec.server.idp.v1.model.AppConfig;
import io.harness.spec.server.idp.v1.model.BackstageEnvSecretVariable;
import io.harness.spec.server.idp.v1.model.BackstageEnvVariable;
import io.harness.spec.server.idp.v1.model.ExportDetails;
import io.harness.spec.server.idp.v1.model.Exports;
import io.harness.spec.server.idp.v1.model.PluginDetailedInfo;
import io.harness.spec.server.idp.v1.model.PluginInfo;
import io.harness.spec.server.idp.v1.model.ProxyHostDetail;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.IDP)
public interface PluginDetailedInfoMapper<S extends PluginDetailedInfo, T extends PluginInfoEntity> {
  S toDto(T entity, AppConfig appConfig, List<BackstageEnvSecretVariable> secrets, List<ProxyHostDetail> hostDetails);
  T fromDto(S dto, String accountIdentifier);

  default void setCommonFieldsDto(PluginInfoEntity entity, PluginDetailedInfo dto, AppConfig appConfig,
      List<BackstageEnvSecretVariable> secrets, List<ProxyHostDetail> hostDetails) {
    boolean isConfigSaved = appConfig != null;
    boolean isEnabled = isConfigSaved && appConfig.isEnabled();
    dto.setPluginDetails(io.harness.idp.plugin.mappers.PluginInfoMapper.toDTO(entity, isEnabled));
    String config;
    if (isEnabled) {
      config = appConfig.getConfigs();
    } else {
      config = (isConfigSaved && appConfig.getConfigs() != null) ? appConfig.getConfigs() : entity.getConfig();
    }
    Exports exports = new Exports();
    ExportsData exportsFromDb = entity.getExports();
    if (exportsFromDb != null && exportsFromDb.getExportDetails() != null) {
      exports.setDefaultEntityTypes(exportsFromDb.getDefaultEntityTypes());
      exports.setCards(PluginInfoEntity.getExportTypeCount(entity, ExportType.CARD));
      exports.setTabContents(PluginInfoEntity.getExportTypeCount(entity, ExportType.TAB_CONTENT));
      exports.setPages(PluginInfoEntity.getExportTypeCount(entity, ExportType.PAGE));
      List<ExportDetails> exportDetailsList = new ArrayList<>();
      for (ExportsData.ExportDetails details : exportsFromDb.getExportDetails()) {
        ExportDetails exportDetails = new ExportDetails();
        exportDetails.setName(details.getName());
        exportDetails.setType(details.getType().toString());
        exportDetails.setAddByDefault(Boolean.valueOf(details.getAddByDefault()));
        exportDetails.setDefaultRoute(details.getDefaultRoute());
        exportDetails.setLayoutSchemaSpecs(details.getLayoutSchemaSpecs());
        exportDetailsList.add(exportDetails);
      }
      exports.setExportDetails(exportDetailsList);
    }
    dto.setSaved(isConfigSaved);
    dto.setExports(exports);
    dto.setConfig(config);
    dto.setEnvVariables(secrets);
    dto.setProxy(hostDetails);
  }

  default void setCommonFieldsEntity(PluginDetailedInfo dto, PluginInfoEntity entity, String accountIdentifier) {
    entity.setAccountIdentifier(accountIdentifier);
    entity.setEnvVariables(
        dto.getEnvVariables().stream().map(BackstageEnvVariable::getEnvName).collect(Collectors.toList()));
    entity.setConfig(dto.getConfig());

    List<ExportsData.ExportDetails> exportDetailsList = new ArrayList<>();
    for (ExportDetails exportDetailsFromDto : dto.getExports().getExportDetails()) {
      ExportsData.ExportDetails exportDetails = ExportsData.ExportDetails.builder()
                                                    .name(exportDetailsFromDto.getName())
                                                    .type(ExportType.valueOf(exportDetailsFromDto.getType()))
                                                    .addByDefault(String.valueOf(exportDetailsFromDto.isAddByDefault()))
                                                    .defaultRoute(exportDetailsFromDto.getDefaultRoute())
                                                    .layoutSchemaSpecs(exportDetailsFromDto.getLayoutSchemaSpecs())
                                                    .build();
      exportDetailsList.add(exportDetails);
    }
    ExportsData exportsData = ExportsData.builder()
                                  .exportDetails(exportDetailsList)
                                  .defaultEntityTypes(dto.getExports().getDefaultEntityTypes())
                                  .build();
    entity.setExports(exportsData);

    PluginInfo pluginDetails = dto.getPluginDetails();
    entity.setSource(pluginDetails.getSource());
    entity.setCategory(pluginDetails.getCategory());
    entity.setDescription(pluginDetails.getDescription());
    entity.setCreator(pluginDetails.getCreatedBy());
    entity.setDocumentation(pluginDetails.getDocumentation());
    entity.setIconUrl(pluginDetails.getIconUrl());
    entity.setImageUrl(pluginDetails.getImageUrl());
    entity.setImages(pluginDetails.getImages());
    entity.setName(pluginDetails.getName());
  }
}
