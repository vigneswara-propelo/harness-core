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
import io.harness.idp.plugin.entities.CustomPluginInfoEntity;
import io.harness.idp.plugin.entities.DefaultPluginInfoEntity;
import io.harness.idp.plugin.entities.PluginInfoEntity;
import io.harness.idp.plugin.entities.PluginRequestEntity;
import io.harness.idp.plugin.mappers.PluginDetailedInfoMapper;
import io.harness.idp.plugin.mappers.PluginInfoMapper;
import io.harness.idp.plugin.mappers.PluginRequestMapper;
import io.harness.idp.plugin.repositories.PluginInfoRepository;
import io.harness.idp.plugin.repositories.PluginRequestRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
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
        savePluginInfo(id);
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
  public void savePluginInfo(CustomPluginDetailedInfo info, String accountIdentifier) {
    PluginDetailedInfoMapper mapper = getMapper(PluginInfo.PluginTypeEnum.CUSTOM);
    PluginInfoEntity entity = mapper.fromDto(info, accountIdentifier);
    pluginInfoRepository.save(entity);
  }

  @Override
  public void updatePluginInfo(String pluginId, CustomPluginDetailedInfo info, String accountIdentifier) {
    PluginDetailedInfoMapper mapper = getMapper(PluginInfo.PluginTypeEnum.CUSTOM);
    PluginInfoEntity entity = mapper.fromDto(info, accountIdentifier);
    pluginInfoRepository.update(pluginId, accountIdentifier, entity);
  }

  public void savePluginInfo(String identifier) throws Exception {
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
}
