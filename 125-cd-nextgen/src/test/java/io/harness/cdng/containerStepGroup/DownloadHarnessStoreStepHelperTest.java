/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.containerStepGroup;

import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.callback.DelegateCallbackToken;
import io.harness.category.element.UnitTests;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.plugininfoproviders.PluginExecutionConfig;
import io.harness.filestore.dto.node.FileNodeDTO;
import io.harness.filestore.dto.node.FileStoreNodeDTO;
import io.harness.filestore.dto.node.FolderNodeDTO;
import io.harness.filestore.service.FileStoreService;
import io.harness.ng.core.filestore.FileUsage;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.plugin.ContainerPortHelper;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class DownloadHarnessStoreStepHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Mock ContainerPortHelper containerPortHelper;

  @Mock PluginExecutionConfig pluginExecutionConfig;

  @Mock FileStoreService fileStoreService;

  @Mock private ContainerStepGroupHelper containerStepGroupHelper;

  public static String MANIFEST_YML = "applications:\n"
      + "- name: test-tas\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances: ((INSTANCES))\n";

  public static String VARS_YML_1 = "MY: order\n"
      + "PCF_APP_NAME: test-tas\n"
      + "INSTANCES: 3\n"
      + "WEB_INSTANCES: 1\n"
      + "ROUTE: route1";

  private final String ACCOUNT_ID = "test-account";
  private final String ORG_ID = "test-org";
  private final String PROJECT_ID = "test-account";

  private static final String STAGE_EXECUTION_ID = "stageExecutionId";

  private final Ambiance ambiance = Ambiance.newBuilder()
                                        .putSetupAbstractions(SetupAbstractionKeys.accountId, ACCOUNT_ID)
                                        .putSetupAbstractions(SetupAbstractionKeys.orgIdentifier, ORG_ID)
                                        .putSetupAbstractions(SetupAbstractionKeys.projectIdentifier, PROJECT_ID)
                                        .setStageExecutionId(STAGE_EXECUTION_ID)
                                        .build();

  @InjectMocks @Spy private DownloadHarnessStoreStepHelper downloadHarnessStoreStepHelper;

  @Before
  public void setup() {}

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetEnvironmentVariables() {
    DownloadHarnessStoreStepParameters downloadHarnessStoreStepParameters =
        DownloadHarnessStoreStepParameters.infoBuilder()
            .downloadPath(ParameterField.createValueField("path"))
            .files(ParameterField.createValueField(Arrays.asList("org:/path/to/tas/manifests")))
            .outputFilePathsContent(ParameterField.createValueField(Arrays.asList("abc")))
            .build();
    doReturn(Optional.of(getFileStoreNode("path/to/tas/manifests/manifest.yaml", "manifest.yaml", MANIFEST_YML)))
        .doReturn(Optional.of(getFileStoreNode("path/to/tas/manifests/vars.yaml", "vars.yaml", VARS_YML_1)))
        .doReturn(Optional.of(getFolderStoreNode("/path/to/tas/manifests", "manifests")))
        .when(fileStoreService)
        .getWithChildrenByPath(any(), any(), any(), any(), eq(true));
    doReturn("abc").when(containerStepGroupHelper).convertToJson(any());

    Map<String, String> environmentVariables = downloadHarnessStoreStepHelper.getEnvironmentVariables(
        ambiance, downloadHarnessStoreStepParameters, "stepIdentifier");
    assertThat(environmentVariables.size()).isEqualTo(3);
  }

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetFileContentsFromManifest() {
    doReturn(Optional.of(getFileStoreNode("path/to/tas/manifests/manifest.yaml", "manifest.yaml", MANIFEST_YML)))
        .doReturn(Optional.of(getFileStoreNode("path/to/tas/manifests/vars.yaml", "vars.yaml", VARS_YML_1)))
        .doReturn(Optional.of(getFolderStoreNode("/path/to/tas/manifests", "manifests")))
        .when(fileStoreService)
        .getWithChildrenByPath(any(), any(), any(), any(), eq(true));

    Map<String, String> files = downloadHarnessStoreStepHelper.getFileContentsFromManifest(
        AmbianceUtils.getNgAccess(ambiance), Arrays.asList("org:/path/to/tas/manifests"));
    assertThat(files.size()).isEqualTo(1);

    Map<String, String> files1 = downloadHarnessStoreStepHelper.getFileContentsFromManifest(
        AmbianceUtils.getNgAccess(ambiance), Arrays.asList("org:/path/to/tas/manifests"));
    assertThat(files.size()).isEqualTo(1);
  }

  private FileStoreNodeDTO getFileStoreNode(String path, String name, String fileContent) {
    return FileNodeDTO.builder()
        .name(name)
        .identifier(name)
        .fileUsage(FileUsage.MANIFEST_FILE)
        .parentIdentifier("folder")
        .content(fileContent)
        .path(path)
        .build();
  }

  private FileStoreNodeDTO getFolderStoreNode(String path, String name) {
    FolderNodeDTO folderNodeDTO =
        FolderNodeDTO.builder().name(name).identifier("identifier").parentIdentifier("tas").path(path).build();
    folderNodeDTO.addChild(getFileStoreNode("path/to/tas/manifests/manifest.yaml", "manifest.yaml", MANIFEST_YML));
    folderNodeDTO.addChild(getFileStoreNode("path/to/tas/manifests/vars.yaml", "vars.yaml", VARS_YML_1));
    return folderNodeDTO;
  }
}