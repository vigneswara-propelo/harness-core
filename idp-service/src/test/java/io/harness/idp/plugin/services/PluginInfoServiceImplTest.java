/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.plugin.services;

import static io.harness.idp.common.Constants.PLUGIN_REQUEST_NOTIFICATION_SLACK_WEBHOOK;
import static io.harness.rule.OwnerRule.SATHISH;
import static io.harness.rule.OwnerRule.VIGNESWARA;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.idp.common.FileUtils;
import io.harness.idp.common.IdpCommonService;
import io.harness.idp.configmanager.service.ConfigEnvVariablesService;
import io.harness.idp.configmanager.service.ConfigManagerService;
import io.harness.idp.configmanager.service.PluginsProxyInfoService;
import io.harness.idp.configmanager.utils.ConfigType;
import io.harness.idp.envvariable.service.BackstageEnvVariableService;
import io.harness.idp.plugin.beans.ExportsData;
import io.harness.idp.plugin.entities.DefaultPluginInfoEntity;
import io.harness.idp.plugin.entities.PluginInfoEntity;
import io.harness.idp.plugin.entities.PluginRequestEntity;
import io.harness.idp.plugin.enums.ExportType;
import io.harness.idp.plugin.mappers.DefaultPluginDetailedInfoMapper;
import io.harness.idp.plugin.mappers.PluginDetailedInfoMapper;
import io.harness.idp.plugin.repositories.PluginInfoRepository;
import io.harness.idp.plugin.repositories.PluginRequestRepository;
import io.harness.rule.Owner;
import io.harness.spec.server.idp.v1.model.PluginDetailedInfo;
import io.harness.spec.server.idp.v1.model.PluginInfo;
import io.harness.spec.server.idp.v1.model.RequestPlugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@OwnedBy(HarnessTeam.IDP)
public class PluginInfoServiceImplTest {
  private PluginInfoServiceImpl pluginInfoServiceImpl;
  @Mock private PluginInfoRepository pluginInfoRepository;
  @Mock private PluginRequestRepository pluginRequestRepository;
  @Mock private ConfigManagerService configManagerService;
  @Mock private ConfigEnvVariablesService configEnvVariablesService;
  @Mock private BackstageEnvVariableService backstageEnvVariableService;
  @Mock private PluginsProxyInfoService pluginsProxyInfoService;
  @Mock private IdpCommonService idpCommonService;
  @Mock private HashMap<String, String> notificationConfigs = new HashMap<>();
  private Map<PluginInfo.PluginTypeEnum, PluginDetailedInfoMapper> mapBinder;
  private final ObjectMapper objectMapper = mock(ObjectMapper.class);

  private static final String ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";
  private static final String PAGER_DUTY_NAME = "PagerDuty";
  private static final String PAGER_DUTY_ID = "pager-duty";
  private static final String HARNESS_CI_CD_NAME = "Harnes CI/CD";
  private static final String HARNESS_CI_CD_ID = "harness-ci-cd";
  private static final String INVALID_PLUGIN_ID = "invalid-plugin";

  private static final String PLUGIN_REQUEST_NAME = "pluginName";
  private static final String PLUGIN_REQUEST_CREATOR = "foo";
  private static final String PLUGIN_REQUEST_PACKAGE_LINK = "https://www.harness.io";
  private static final String PLUGIN_REQUEST_DOC_LINK = "https://www.harness.io";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    PluginDetailedInfoMapper<?, ?> pluginDetailedInfoMapper = new DefaultPluginDetailedInfoMapper();
    mapBinder = Map.of(PluginInfo.PluginTypeEnum.DEFAULT, pluginDetailedInfoMapper);
    pluginInfoServiceImpl = new PluginInfoServiceImpl(pluginInfoRepository, pluginRequestRepository,
        configManagerService, configEnvVariablesService, backstageEnvVariableService, pluginsProxyInfoService,
        idpCommonService, "", notificationConfigs, mapBinder);
  }

  @Test
  @Owner(developers = VIGNESWARA)
  @Category(UnitTests.class)
  public void testGetAllPluginsInfo() {
    List<PluginInfoEntity> pluginInfoEntityList = new ArrayList<>();
    pluginInfoEntityList.add(getPagerDutyInfoEntity());
    pluginInfoEntityList.add(getHarnessCICDInfoEntity());
    when(pluginInfoRepository.findByIdentifierInAndAccountIdentifierOrTypeAndAccountIdentifier(
             any(), any(), any(), any()))
        .thenReturn(pluginInfoEntityList);
    Map<String, Boolean> map = new HashMap<>();
    map.put(PAGER_DUTY_ID, false);
    map.put(HARNESS_CI_CD_ID, true);
    when(configManagerService.getAllPluginIdsMap(ACCOUNT_ID)).thenReturn(map);
    List<PluginInfo> pluginDTOs = pluginInfoServiceImpl.getAllPluginsInfo(ACCOUNT_ID);
    assertEquals(2, pluginDTOs.size());
    assertFalse(pluginDTOs.get(0).isEnabled());
    assertTrue(pluginDTOs.get(1).isEnabled());
  }

  @Test
  @Owner(developers = VIGNESWARA)
  @Category(UnitTests.class)
  public void testGetPluginDetailedInfo() {
    when(pluginInfoRepository.findByIdentifierAndAccountIdentifierIn(PAGER_DUTY_ID, Collections.singleton(ACCOUNT_ID)))
        .thenReturn(Optional.ofNullable(getPagerDutyInfoEntity()));
    when(configManagerService.getAppConfig(ACCOUNT_ID, PAGER_DUTY_ID, ConfigType.PLUGIN)).thenReturn(null);
    when(pluginsProxyInfoService.getProxyHostDetailsForPluginId(ACCOUNT_ID, PAGER_DUTY_ID))
        .thenReturn(new ArrayList<>());
    PluginDetailedInfo pluginDetailedInfo =
        pluginInfoServiceImpl.getPluginDetailedInfo(PAGER_DUTY_ID, ACCOUNT_ID, false);
    assertNotNull(pluginDetailedInfo);
    assertFalse(pluginDetailedInfo.getPluginDetails().isEnabled());
    assertEquals(1, (int) pluginDetailedInfo.getExports().getCards());
    assertEquals(0, (int) pluginDetailedInfo.getExports().getPages());
    assertEquals(0, (int) pluginDetailedInfo.getExports().getTabContents());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = VIGNESWARA)
  @Category(UnitTests.class)
  public void testGetPluginDetailedInfoThrowsException() {
    when(pluginInfoRepository.findByIdentifier(INVALID_PLUGIN_ID)).thenReturn(Optional.empty());
    pluginInfoServiceImpl.getPluginDetailedInfo(INVALID_PLUGIN_ID, ACCOUNT_ID, false);
  }

  @Test
  @Owner(developers = VIGNESWARA)
  @Category(UnitTests.class)
  public void testSaveAllPluginInfo() {
    String schema = "identifier: github-pull-requests-board\n"
        + "name: GitHub Pull Requests Board\n"
        + "description: View all open GitHub pull requests owned by your team in Backstage.\n"
        + "creator: DAZN\n"
        + "category: Source Control Mgmt\n"
        + "source: https://github.com/backstage/backstage/tree/master/plugins/github-pull-requests-board";
    PluginInfoEntity pluginInfoEntity = DefaultPluginInfoEntity.builder().build();
    mockStatic(FileUtils.class);
    when(FileUtils.readFile(any(), any(), any())).thenReturn(schema);
    when(pluginInfoRepository.saveOrUpdate(any(PluginInfoEntity.class))).thenReturn(pluginInfoEntity);
    pluginInfoServiceImpl.saveAllPluginInfo();
    verify(pluginInfoRepository, times(19)).saveOrUpdate(any(PluginInfoEntity.class));
  }

  @Test
  @Owner(developers = VIGNESWARA)
  @Category(UnitTests.class)
  public void testDeleteAllPluginInfo() {
    doNothing().when(pluginInfoRepository).deleteAll();
    pluginInfoServiceImpl.deleteAllPluginInfo();
    verify(pluginInfoRepository).deleteAll();
  }

  @Test
  @Owner(developers = SATHISH)
  @Category(UnitTests.class)
  public void testSavePluginRequest() {
    PluginRequestEntity pluginRequestEntity =
        PluginRequestEntity.builder().createdBy(EmbeddedUser.builder().build()).build();
    when(pluginRequestRepository.save(any(PluginRequestEntity.class))).thenReturn(pluginRequestEntity);
    when(notificationConfigs.get(PLUGIN_REQUEST_NOTIFICATION_SLACK_WEBHOOK)).thenReturn("");
    doNothing().when(idpCommonService).sendSlackNotification(any());
    pluginInfoServiceImpl.savePluginRequest(ACCOUNT_ID, getRequestPlugin());
    verify(pluginRequestRepository, times(1)).save(any(PluginRequestEntity.class));
  }

  @Test
  @Owner(developers = SATHISH)
  @Category(UnitTests.class)
  public void testGetPluginRequests() {
    when(pluginRequestRepository.findAll(any(), any())).thenReturn(getPagePluginRequestEntity());
    Page<PluginRequestEntity> pluginRequestEntityPage = pluginInfoServiceImpl.getPluginRequests(ACCOUNT_ID, 0, 10);
    assertEquals(1, pluginRequestEntityPage.getTotalElements());
    assertEquals(1, pluginRequestEntityPage.getContent().size());
    assertThat(pluginRequestEntityPage.getContent().get(0).getName()).isEqualTo(PLUGIN_REQUEST_NAME);
    assertThat(pluginRequestEntityPage.getContent().get(0).getCreator()).isEqualTo(PLUGIN_REQUEST_CREATOR);
    assertThat(pluginRequestEntityPage.getContent().get(0).getPackageLink()).isEqualTo(PLUGIN_REQUEST_PACKAGE_LINK);
    assertThat(pluginRequestEntityPage.getContent().get(0).getDocLink()).isEqualTo(PLUGIN_REQUEST_DOC_LINK);
  }

  private PluginInfoEntity getPagerDutyInfoEntity() {
    List<ExportsData.ExportDetails> exportDetails = new ArrayList<>();
    ExportsData.ExportDetails export = new ExportsData.ExportDetails();
    export.setType(ExportType.CARD);
    export.setName("EntityPagerDutyCard");
    exportDetails.add(export);
    ExportsData exportsData = new ExportsData();
    exportsData.setExportDetails(exportDetails);
    DefaultPluginInfoEntity entity = DefaultPluginInfoEntity.builder().core(false).build();
    entity.setName(PAGER_DUTY_NAME);
    entity.setIdentifier(PAGER_DUTY_ID);
    entity.setExports(exportsData);
    entity.setType(PluginInfo.PluginTypeEnum.DEFAULT);
    return entity;
  }

  private PluginInfoEntity getHarnessCICDInfoEntity() {
    DefaultPluginInfoEntity entity = DefaultPluginInfoEntity.builder().core(false).build();
    entity.setName(HARNESS_CI_CD_NAME);
    entity.setIdentifier(HARNESS_CI_CD_ID);
    entity.setExports(new ExportsData());
    return entity;
  }

  private RequestPlugin getRequestPlugin() {
    RequestPlugin requestPlugin = new RequestPlugin();
    requestPlugin.setName(PLUGIN_REQUEST_NAME);
    requestPlugin.setCreator(PLUGIN_REQUEST_CREATOR);
    requestPlugin.setPackageLink(PLUGIN_REQUEST_PACKAGE_LINK);
    requestPlugin.setDocLink(PLUGIN_REQUEST_DOC_LINK);
    return requestPlugin;
  }

  private Page<PluginRequestEntity> getPagePluginRequestEntity() {
    PluginRequestEntity pluginRequestEntity = PluginRequestEntity.builder()
                                                  .name(PLUGIN_REQUEST_NAME)
                                                  .creator(PLUGIN_REQUEST_CREATOR)
                                                  .packageLink(PLUGIN_REQUEST_PACKAGE_LINK)
                                                  .docLink(PLUGIN_REQUEST_DOC_LINK)
                                                  .build();
    List<PluginRequestEntity> pluginRequestEntityList = new ArrayList<>();
    pluginRequestEntityList.add(pluginRequestEntity);
    return new PageImpl<>(pluginRequestEntityList);
  }
}
