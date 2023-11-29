/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.plugininfoproviders;

import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.cdng.containerStepGroup.ContainerStepGroupHelper;
import io.harness.cdng.containerStepGroup.DownloadAwsS3StepHelper;
import io.harness.cdng.containerStepGroup.DownloadAwsS3StepInfo;
import io.harness.cdng.pipeline.steps.CdAbstractStepNode;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.ImageDetails;
import io.harness.pms.contracts.plan.PluginCreationRequest;
import io.harness.pms.contracts.plan.PluginCreationResponseWrapper;
import io.harness.pms.contracts.plan.PluginDetails;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;
import io.harness.steps.container.execution.plugin.StepImageConfig;
import io.harness.utils.IdentifierRefHelper;
import io.harness.yaml.extended.ci.container.ContainerResource;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class DownloadAwsS3PluginInfoProviderTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private PluginExecutionConfig pluginExecutionConfig;
  @Mock private DownloadAwsS3StepHelper downloadAwsS3StepHelper;

  @Mock ContainerStepGroupHelper containerStepGroupHelper;
  @InjectMocks @Spy private DownloadAwsS3PluginInfoProvider downloadAwsS3PluginInfoProvider;

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetPluginInfo() throws IOException {
    String accountId = "accountId";
    Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();

    String jsonNode = "jsonNdod";
    PluginCreationRequest pluginCreationRequest = PluginCreationRequest.newBuilder().setStepJsonNode(jsonNode).build();
    CdAbstractStepNode cdAbstractStepNode = mock(CdAbstractStepNode.class);
    Mockito.mockStatic(YamlUtils.class);
    when(YamlUtils.read(anyString(), (Class<Object>) any())).thenReturn(cdAbstractStepNode);
    doReturn("identifier").when(cdAbstractStepNode).getIdentifier();
    doReturn("name").when(cdAbstractStepNode).getName();
    doReturn("uuid").when(cdAbstractStepNode).getUuid();
    String connectorRef = "ref";

    DownloadAwsS3StepInfo downloadAwsS3StepInfo = DownloadAwsS3StepInfo.infoBuilder()
                                                      .resources(ContainerResource.builder().build())
                                                      .runAsUser(ParameterField.<Integer>builder().value(1).build())
                                                      .bucketName(ParameterField.createValueField("bucket"))
                                                      .region(ParameterField.createValueField("us-east-1"))
                                                      .downloadPath(ParameterField.createValueField("path"))
                                                      .connectorRef(ParameterField.createValueField("ref"))
                                                      .build();
    doReturn(downloadAwsS3StepInfo).when(cdAbstractStepNode).getStepSpecType();

    Mockito.mockStatic(AmbianceUtils.class);
    NGAccess ngAccess = mock(NGAccess.class);
    when(AmbianceUtils.getNgAccess(any())).thenReturn(ngAccess);

    Mockito.mockStatic(IdentifierRefHelper.class);
    IdentifierRef identifierRef = mock(IdentifierRef.class);
    when(ngAccess.getAccountIdentifier()).thenReturn("account");
    when(ngAccess.getOrgIdentifier()).thenReturn("account");
    when(ngAccess.getProjectIdentifier()).thenReturn("account");
    when(IdentifierRefHelper.getIdentifierRef(any(), any(), any(), any())).thenReturn(identifierRef);

    when(identifierRef.getAccountIdentifier()).thenReturn("account");
    when(identifierRef.getOrgIdentifier()).thenReturn("account");
    when(identifierRef.getProjectIdentifier()).thenReturn("account");
    when(identifierRef.getIdentifier()).thenReturn("account");

    PluginDetails.Builder pluginDetailsBuilder = PluginDetails.newBuilder();
    ImageDetails imageDetails = mock(ImageDetails.class);
    Mockito.mockStatic(PluginInfoProviderHelper.class);
    when(PluginInfoProviderHelper.buildPluginDetails(any(), any(), any(), anyBoolean()))
        .thenReturn(pluginDetailsBuilder);
    when(PluginInfoProviderHelper.getImageDetails(any(), any(), any())).thenReturn(imageDetails);

    doReturn(StepImageConfig.builder().image("harnessdev/download-aws-s3:1.0.0-rootless-linux").build())
        .when(pluginExecutionConfig)
        .getDownloadAwsS3Config();

    doReturn(new HashMap<>()).when(downloadAwsS3StepHelper).getEnvironmentVariables(any(), any(), any());
    doReturn(new HashMap<>()).when(containerStepGroupHelper).getEnvVarsWithSecretRef(any());
    doReturn(new HashMap<>()).when(containerStepGroupHelper).validateEnvVariables(any());

    PluginCreationResponseWrapper pluginCreationResponseWrapper =
        downloadAwsS3PluginInfoProvider.getPluginInfo(pluginCreationRequest, Collections.emptySet(), ambiance);

    assertThat(pluginCreationResponseWrapper.getStepInfo().getIdentifier()).isEqualTo("identifier");
    assertThat(pluginCreationResponseWrapper.getStepInfo().getName()).isEqualTo("name");
    assertThat(pluginCreationResponseWrapper.getStepInfo().getUuid()).isEqualTo("uuid");
  }
}
