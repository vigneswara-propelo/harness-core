/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.containerStepGroup;

import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.callback.DelegateCallbackToken;
import io.harness.category.element.UnitTests;
import io.harness.cdng.plugininfoproviders.PluginExecutionConfig;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.plugin.ContainerPortHelper;
import io.harness.pms.sdk.core.plugin.ContainerUnitStepUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.rule.Owner;
import io.harness.steps.container.execution.plugin.StepImageConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class DownloadHarnessStoreStepTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Mock DownloadHarnessStoreStepHelper downloadHarnessStoreStepHelper;

  @Mock ContainerPortHelper containerPortHelper;

  @Mock PluginExecutionConfig pluginExecutionConfig;

  @Mock private ContainerStepGroupHelper containerStepGroupHelper;

  @InjectMocks @Spy private DownloadHarnessStoreStep downloadHarnessStoreStep;

  @Before
  public void setup() {}

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetSerialisedStep() {
    String accountId = "accountId";
    int port = 1;
    String callbackToken = "token";
    String displayName = "name";
    String id = "id";
    String logKey = "logKey";
    Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();
    DownloadHarnessStoreStepParameters stepParameters =
        DownloadHarnessStoreStepParameters.infoBuilder()
            .downloadPath(ParameterField.createValueField("path"))
            .files(ParameterField.createValueField(Arrays.asList("abc")))
            .build();
    StepElementParameters stepElementParameters = StepElementParameters.builder()
                                                      .name("name")
                                                      .spec(stepParameters)
                                                      .timeout(ParameterField.createValueField("10m"))
                                                      .build();

    Mockito.mockStatic(AmbianceUtils.class);
    when(AmbianceUtils.obtainStepIdentifier(any())).thenReturn("identifier");

    doReturn(123).when(containerPortHelper).getPort(any(), any(), anyBoolean());

    doReturn(StepImageConfig.builder().image("harnessdev/download-harness-store:1.0.0-rootless-linux").build())
        .when(pluginExecutionConfig)
        .getDownloadHarnessStoreConfig();

    doReturn(new HashMap<>()).when(downloadHarnessStoreStepHelper).getEnvironmentVariables(any(), any(), any());
    doReturn(new HashMap<>()).when(containerStepGroupHelper).removeAllEnvVarsWithSecretRef(any());
    doReturn(new HashMap<>()).when(containerStepGroupHelper).validateEnvVariables(any());

    UnitStep unitStep = mock(UnitStep.class);
    doReturn(accountId).when(unitStep).getAccountId();
    doReturn(port).when(unitStep).getContainerPort();
    doReturn(callbackToken).when(unitStep).getCallbackToken();
    doReturn(displayName).when(unitStep).getDisplayName();
    doReturn(id).when(unitStep).getId();
    doReturn(logKey).when(unitStep).getLogKey();

    MockedStatic<ContainerUnitStepUtils> mock = Mockito.mockStatic(ContainerUnitStepUtils.class);
    when(ContainerUnitStepUtils.serializeStepWithStepParameters(anyInt(), anyString(), anyString(), anyString(),
             anyLong(), anyString(), anyString(), any(), any(), any(), anyString(), any()))
        .thenReturn(unitStep);

    doReturn(DelegateCallbackToken.newBuilder().setToken("token").build()).when(delegateCallbackTokenSupplier).get();

    long timeout = 1000;
    String parkedTaskId = "parkedTaskId";

    downloadHarnessStoreStep.getSerialisedStep(
        ambiance, stepElementParameters, accountId, logKey, timeout, parkedTaskId);
    verify(pluginExecutionConfig, times(1)).getDownloadHarnessStoreConfig();
  }
}