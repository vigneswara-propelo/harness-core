/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.plugin.services;

import static io.harness.idp.common.CommonUtils.addGlobalAccountIdentifierAlong;
import static io.harness.idp.common.Constants.CUSTOM_PLUGIN;
import static io.harness.idp.common.Constants.GLOBAL_ACCOUNT_ID;
import static io.harness.idp.common.Constants.PLUGIN_REQUEST_NOTIFICATION_SLACK_WEBHOOK;
import static io.harness.idp.plugin.beans.FileType.ICON;
import static io.harness.idp.plugin.beans.FileType.SCREENSHOT;
import static io.harness.notification.templates.PredefinedTemplate.IDP_PLUGIN_REQUESTS_NOTIFICATION_SLACK;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.idp.common.Constants;
import io.harness.idp.common.FileUtils;
import io.harness.idp.common.IdpCommonService;
import io.harness.idp.configmanager.service.ConfigEnvVariablesService;
import io.harness.idp.configmanager.service.ConfigManagerService;
import io.harness.idp.configmanager.service.PluginsProxyInfoService;
import io.harness.idp.configmanager.utils.ConfigManagerUtils;
import io.harness.idp.configmanager.utils.ConfigType;
import io.harness.idp.envvariable.service.BackstageEnvVariableService;
import io.harness.idp.plugin.beans.FileType;
import io.harness.idp.plugin.entities.CustomPluginInfoEntity;
import io.harness.idp.plugin.entities.DefaultPluginInfoEntity;
import io.harness.idp.plugin.entities.PluginInfoEntity;
import io.harness.idp.plugin.entities.PluginRequestEntity;
import io.harness.idp.plugin.mappers.CustomPluginDetailedInfoMapper;
import io.harness.idp.plugin.mappers.PluginDetailedInfoMapper;
import io.harness.idp.plugin.mappers.PluginInfoMapper;
import io.harness.idp.plugin.mappers.PluginRequestMapper;
import io.harness.idp.plugin.repositories.PluginInfoRepository;
import io.harness.idp.plugin.repositories.PluginRequestRepository;
import io.harness.idp.plugin.utils.GcpStorageUtil;
import io.harness.notification.Team;
import io.harness.notification.channeldetails.SlackChannel;
import io.harness.spec.server.idp.v1.model.AppConfig;
import io.harness.spec.server.idp.v1.model.BackstageEnvSecretVariable;
import io.harness.spec.server.idp.v1.model.CustomPluginDetailedInfo;
import io.harness.spec.server.idp.v1.model.PluginDetailedInfo;
import io.harness.spec.server.idp.v1.model.PluginInfo;
import io.harness.spec.server.idp.v1.model.ProxyHostDetail;
import io.harness.spec.server.idp.v1.model.RequestPlugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.IDP)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class PluginInfoServiceImpl implements PluginInfoService {
  private static final String METADATA_FOLDER = "metadata/";
  private static final String YAML_EXT = ".yaml";
  private static final int RANDOM_STRING_LENGTH = 6;
  private static final String CUSTOM_PLUGIN_IDENTIFIER_FORMAT = "my_custom_plugin_%s";
  public static final String CUSTOM_PLUGINS_BUCKET_NAME = "idp-custom-plugins";
  private static final String PATH_SEPARATOR = "/";
  private static final String FILE_NAME_SEPARATOR = "_";
  private static final String ZIP_EXTENSION = "zip";
  private static final String TAR_GZ_EXTENSION = "tar.gz";
  private static final String TAR_BZ2_EXTENSION = "tar.bz2";
  private static final String JPEG_EXTENSION = "jpeg";
  private static final String JPG_EXTENSION = "jpg";
  private static final String PNG_EXTENSION = "png";
  private static final List<String> SUPPORTED_PLUGIN_FILE_FORMATS =
      Arrays.asList(ZIP_EXTENSION, TAR_GZ_EXTENSION, TAR_BZ2_EXTENSION);
  private static final List<String> SUPPORTED_IMAGE_FILE_FORMATS =
      Arrays.asList(JPEG_EXTENSION, JPG_EXTENSION, PNG_EXTENSION);
  private PluginInfoRepository pluginInfoRepository;
  private PluginRequestRepository pluginRequestRepository;
  private ConfigManagerService configManagerService;
  private ConfigEnvVariablesService configEnvVariablesService;
  private BackstageEnvVariableService backstageEnvVariableService;
  private PluginsProxyInfoService pluginsProxyInfoService;
  private IdpCommonService idpCommonService;
  @Inject @Named("env") private String env;
  @Inject @Named("notificationConfigs") HashMap<String, String> notificationConfigs;
  Map<PluginInfo.PluginTypeEnum, PluginDetailedInfoMapper> pluginDetailedInfoMapperMap;
  private GcpStorageUtil gcpStorageUtil;

  @Override
  public List<PluginInfo> getAllPluginsInfo(String accountId) {
    List<PluginInfoEntity> plugins =
        pluginInfoRepository.findByIdentifierInAndAccountIdentifierOrTypeAndAccountIdentifier(
            Constants.pluginIds, GLOBAL_ACCOUNT_ID, PluginInfo.PluginTypeEnum.CUSTOM, accountId);
    List<PluginInfo> pluginDTOs = new ArrayList<>();

    Map<String, Boolean> map = configManagerService.getAllPluginIdsMap(accountId);
    plugins.forEach(pluginInfoEntity -> {
      boolean isEnabled =
          map.containsKey(pluginInfoEntity.getIdentifier()) && map.get(pluginInfoEntity.getIdentifier());
      pluginDTOs.add(PluginInfoMapper.toDTO(pluginInfoEntity, isEnabled));
    });
    return pluginDTOs;
  }

  @Override
  public PluginDetailedInfo getPluginDetailedInfo(String identifier, String harnessAccount, boolean meta) {
    PluginInfoEntity pluginEntity;
    AppConfig appConfig = null;

    if (meta) {
      String schema = FileUtils.readFile(METADATA_FOLDER, CUSTOM_PLUGIN, YAML_EXT);
      ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
      try {
        pluginEntity = objectMapper.readValue(schema, CustomPluginInfoEntity.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Could not read default custom plugin metadata", e);
      }
    } else {
      Optional<PluginInfoEntity> pluginInfoEntity = pluginInfoRepository.findByIdentifierAndAccountIdentifierIn(
          identifier, addGlobalAccountIdentifierAlong(harnessAccount));
      if (pluginInfoEntity.isEmpty()) {
        throw new InvalidRequestException(String.format(
            "Plugin Info not found for plugin identifier [%s] for account [%s]", identifier, harnessAccount));
      }
      pluginEntity = pluginInfoEntity.get();
      appConfig = configManagerService.getAppConfig(harnessAccount, identifier, ConfigType.PLUGIN);
      if (pluginEntity.getIdentifier().equals("harness-ci-cd") && appConfig == null) {
        pluginEntity.setConfig(ConfigManagerUtils.getHarnessCiCdAppConfig(env));
      }
    }

    List<BackstageEnvSecretVariable> backstageEnvSecretVariables =
        getPluginSecrets(appConfig, pluginEntity, harnessAccount, identifier);
    List<ProxyHostDetail> proxyHostDetails =
        pluginsProxyInfoService.getProxyHostDetailsForPluginId(harnessAccount, identifier);
    return getMapper(pluginEntity.getType())
        .toDto(pluginEntity, appConfig, backstageEnvSecretVariables, proxyHostDetails);
  }

  @Override
  public void saveAllPluginInfo() {
    Constants.pluginIds.forEach(id -> {
      try {
        saveDefaultPluginInfo(id);
      } catch (Exception e) {
        String errorMessage = String.format("Error occurred while saving plugin details for pluginId: [%s]", id);
        log.error(errorMessage, e);
      }
    });
  }

  @Override
  public void deleteAllPluginInfo() {
    pluginInfoRepository.deleteAll();
  }

  @Override
  public RequestPlugin savePluginRequest(String harnessAccount, RequestPlugin pluginRequest) {
    PluginRequestEntity pluginRequestEntity = PluginRequestMapper.fromDTO(harnessAccount, pluginRequest);
    pluginRequestEntity = pluginRequestRepository.save(pluginRequestEntity);
    sendSlackNotificationForPluginRequest(harnessAccount, pluginRequestEntity);
    return PluginRequestMapper.toDTO(pluginRequestEntity);
  }

  @Override
  public Page<PluginRequestEntity> getPluginRequests(String harnessAccount, int page, int limit) {
    Criteria criteria = createCriteriaForGetPluginRequests(harnessAccount);
    Pageable pageable = PageRequest.of(page, limit);
    return pluginRequestRepository.findAll(criteria, pageable);
  }

  @Override
  public CustomPluginDetailedInfo generateIdentifierAndSaveCustomPluginInfo(String accountIdentifier) {
    CustomPluginInfoEntity entity = CustomPluginInfoEntity.builder().build();
    entity.setType(PluginInfo.PluginTypeEnum.CUSTOM);
    entity.setAccountIdentifier(accountIdentifier);
    entity.setIdentifier(
        String.format(CUSTOM_PLUGIN_IDENTIFIER_FORMAT, RandomStringUtils.randomAlphanumeric(RANDOM_STRING_LENGTH)));
    CustomPluginInfoEntity savedEntity = pluginInfoRepository.save(entity);
    return buildDtoWithAdditionalDetails(savedEntity, accountIdentifier);
  }

  @Override
  public CustomPluginDetailedInfo updatePluginInfo(
      String pluginId, CustomPluginDetailedInfo info, String accountIdentifier) {
    CustomPluginDetailedInfoMapper mapper = new CustomPluginDetailedInfoMapper();
    CustomPluginInfoEntity entity = mapper.fromDto(info, accountIdentifier);
    CustomPluginInfoEntity updatedEntity =
        (CustomPluginInfoEntity) pluginInfoRepository.update(pluginId, accountIdentifier, entity);
    if (updatedEntity == null) {
      throw new NotFoundException(
          String.format("Could not find plugin with identifier %s in account %s", pluginId, accountIdentifier));
    }
    return buildDtoWithAdditionalDetails(updatedEntity, accountIdentifier);
  }

  @Override
  public CustomPluginDetailedInfo uploadFile(String pluginId, String fileType, InputStream fileInputStream,
      FormDataContentDisposition fileDetail, String harnessAccount) {
    String fileExtension = FilenameUtils.getExtension(fileDetail.getFileName());
    if (!fileExtension.isBlank() && !isFileFormatSupported(fileType, fileExtension)) {
      throw new UnsupportedOperationException(
          "File format " + fileExtension + " is not supported. Plugin " + pluginId + ". Account " + harnessAccount);
    }

    String filePath = getFilePath(fileType, harnessAccount);
    String fileName = getFileNamePrefix(fileType, pluginId, harnessAccount)
        + RandomStringUtils.randomAlphanumeric(RANDOM_STRING_LENGTH) + "." + fileExtension;
    String gcsBucketUrl =
        gcpStorageUtil.uploadFileToGcs(CUSTOM_PLUGINS_BUCKET_NAME, filePath, fileName, fileInputStream);

    Optional<PluginInfoEntity> entityOpt =
        pluginInfoRepository.findByIdentifierAndAccountIdentifierIn(pluginId, Collections.singleton(harnessAccount));
    if (entityOpt.isEmpty()) {
      throw new NotFoundException(
          String.format("Could not find plugin details for plugin id %s and account %s", pluginId, harnessAccount));
    }
    PluginInfoEntity entity = entityOpt.get();
    CustomPluginDetailedInfoMapper mapper = new CustomPluginDetailedInfoMapper();
    mapper.addFileUploadDetails(entity, fileType, gcsBucketUrl);
    CustomPluginInfoEntity updatedEntity =
        (CustomPluginInfoEntity) pluginInfoRepository.update(pluginId, harnessAccount, entity);
    if (updatedEntity == null) {
      throw new NotFoundException(
          String.format("Could not find plugin with identifier %s in account %s", pluginId, harnessAccount));
    }
    return buildDtoWithAdditionalDetails(updatedEntity, harnessAccount);
  }

  @Override
  public CustomPluginDetailedInfo deleteFile(String pluginId, String fileType, String fileUrl, String harnessAccount) {
    CustomPluginDetailedInfoMapper mapper = new CustomPluginDetailedInfoMapper();
    gcpStorageUtil.deleteFileFromGcs(fileUrl);
    Optional<PluginInfoEntity> entityOpt =
        pluginInfoRepository.findByIdentifierAndAccountIdentifierIn(pluginId, Collections.singleton(harnessAccount));
    if (entityOpt.isEmpty()) {
      throw new NotFoundException(
          String.format("Could not find plugin details for plugin id %s and account %s", pluginId, harnessAccount));
    }
    PluginInfoEntity entity = entityOpt.get();
    mapper.removeFileDetails(entity, fileType, fileUrl);
    CustomPluginInfoEntity updatedEntity =
        (CustomPluginInfoEntity) pluginInfoRepository.update(pluginId, harnessAccount, entity);
    if (updatedEntity == null) {
      throw new NotFoundException(
          String.format("Could not find plugin with identifier %s in account %s", pluginId, harnessAccount));
    }
    return buildDtoWithAdditionalDetails(updatedEntity, harnessAccount);
  }

  private boolean isFileFormatSupported(String fileType, String extension) {
    switch (FileType.valueOf(fileType)) {
      case ZIP:
        return SUPPORTED_PLUGIN_FILE_FORMATS.contains(extension);
      case ICON:
      case SCREENSHOT:
        return SUPPORTED_IMAGE_FILE_FORMATS.contains(extension);
      default:
        throw new UnsupportedOperationException("File type " + fileType + " is not supported");
    }
  }

  private String getFileNamePrefix(String fileType, String pluginId, String harnessAccount) {
    switch (FileType.valueOf(fileType)) {
      case ZIP:
        return pluginId + FILE_NAME_SEPARATOR;
      case ICON:
        return harnessAccount + FILE_NAME_SEPARATOR + pluginId + ICON.name() + FILE_NAME_SEPARATOR;
      case SCREENSHOT:
        return harnessAccount + FILE_NAME_SEPARATOR + pluginId + SCREENSHOT.name() + FILE_NAME_SEPARATOR;
      default:
        throw new UnsupportedOperationException("File type " + fileType + " is not supported");
    }
  }

  private String getFilePath(String fileType, String harnessAccount) {
    switch (FileType.valueOf(fileType)) {
      case ZIP:
        return "plugins" + PATH_SEPARATOR + harnessAccount;
      case ICON:
      case SCREENSHOT:
        return "static";
      default:
        throw new UnsupportedOperationException("File type " + fileType + " is not supported");
    }
  }

  public void saveDefaultPluginInfo(String identifier) throws Exception {
    String schema = FileUtils.readFile(METADATA_FOLDER, identifier, YAML_EXT);
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    PluginInfoEntity pluginInfoEntity = objectMapper.readValue(schema, DefaultPluginInfoEntity.class);
    pluginInfoRepository.saveOrUpdate(pluginInfoEntity);
  }

  private List<BackstageEnvSecretVariable> getPluginSecrets(
      AppConfig appConfig, PluginInfoEntity pluginEntity, String harnessAccount, String identifier) {
    List<BackstageEnvSecretVariable> backstageEnvSecretVariables = new ArrayList<>();
    if (appConfig != null) {
      List<String> envNames =
          configEnvVariablesService.getAllEnvVariablesForAccountIdentifierAndPluginId(harnessAccount, identifier);
      if (CollectionUtils.isNotEmpty(envNames)) {
        backstageEnvSecretVariables =
            backstageEnvVariableService.getAllSecretIdentifierForMultipleEnvVariablesInAccount(
                harnessAccount, envNames);
      }
    } else if (pluginEntity.getEnvVariables() != null) {
      for (String envVariable : pluginEntity.getEnvVariables()) {
        BackstageEnvSecretVariable backstageEnvSecretVariable = new BackstageEnvSecretVariable();
        backstageEnvSecretVariable.setEnvName(envVariable);
        backstageEnvSecretVariable.setHarnessSecretIdentifier(null);
        backstageEnvSecretVariables.add(backstageEnvSecretVariable);
      }
    }
    return backstageEnvSecretVariables;
  }

  private Criteria createCriteriaForGetPluginRequests(String harnessAccount) {
    Criteria criteria = new Criteria();
    criteria.and(PluginRequestEntity.PluginRequestKeys.accountIdentifier).is(harnessAccount);
    return criteria;
  }

  private void sendSlackNotificationForPluginRequest(String harnessAccount, PluginRequestEntity pluginRequestEntity) {
    SlackChannel slackChannel =
        SlackChannel.builder()
            .accountId(harnessAccount)
            .userGroups(Collections.emptyList())
            .templateId(IDP_PLUGIN_REQUESTS_NOTIFICATION_SLACK.getIdentifier())
            .templateData(pluginRequestEntity.toMap())
            .team(Team.IDP)
            .webhookUrls(Collections.singletonList(notificationConfigs.get(PLUGIN_REQUEST_NOTIFICATION_SLACK_WEBHOOK)))
            .build();
    idpCommonService.sendSlackNotification(slackChannel);
  }

  private PluginDetailedInfoMapper getMapper(PluginInfo.PluginTypeEnum pluginType) {
    PluginDetailedInfoMapper mapper = pluginDetailedInfoMapperMap.get(pluginType);
    if (mapper == null) {
      throw new InvalidRequestException("Plugin type not set");
    }
    return mapper;
  }

  private CustomPluginDetailedInfo buildDtoWithAdditionalDetails(PluginInfoEntity pluginEntity, String harnessAccount) {
    CustomPluginDetailedInfoMapper mapper = new CustomPluginDetailedInfoMapper();
    AppConfig appConfig =
        configManagerService.getAppConfig(harnessAccount, pluginEntity.getIdentifier(), ConfigType.PLUGIN);
    if (pluginEntity.getIdentifier().equals("harness-ci-cd") && appConfig == null) {
      pluginEntity.setConfig(ConfigManagerUtils.getHarnessCiCdAppConfig(env));
    }
    List<BackstageEnvSecretVariable> backstageEnvSecretVariables =
        getPluginSecrets(appConfig, pluginEntity, harnessAccount, pluginEntity.getIdentifier());
    List<ProxyHostDetail> proxyHostDetails =
        pluginsProxyInfoService.getProxyHostDetailsForPluginId(harnessAccount, pluginEntity.getIdentifier());
    return mapper.toDto(
        (CustomPluginInfoEntity) pluginEntity, appConfig, backstageEnvSecretVariables, proxyHostDetails);
  }
}
