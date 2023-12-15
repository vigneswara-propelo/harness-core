/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.aws.sam;

import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.nodes.GitCloneStepNode;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.cdng.containerStepGroup.DownloadAwsS3Step;
import io.harness.cdng.containerStepGroup.DownloadAwsS3StepInfo;
import io.harness.cdng.containerStepGroup.DownloadAwsS3StepNode;
import io.harness.cdng.containerStepGroup.DownloadHarnessStoreStep;
import io.harness.cdng.containerStepGroup.DownloadHarnessStoreStepInfo;
import io.harness.cdng.containerStepGroup.DownloadHarnessStoreStepNode;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.manifest.steps.outcome.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.AwsSamDirectoryManifestOutcome;
import io.harness.cdng.manifest.yaml.BitbucketStore;
import io.harness.cdng.manifest.yaml.GitLabStore;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.cdng.manifest.yaml.harness.HarnessStore;
import io.harness.cdng.pipeline.steps.CdAbstractStepNode;
import io.harness.cdng.plugininfoproviders.DownloadAwsS3PluginInfoProvider;
import io.harness.cdng.plugininfoproviders.DownloadHarnessStorePluginInfoProvider;
import io.harness.cdng.plugininfoproviders.GitClonePluginInfoProvider;
import io.harness.delegate.task.stepstatus.StepExecutionStatus;
import io.harness.delegate.task.stepstatus.StepMapOutput;
import io.harness.delegate.task.stepstatus.StepOutput;
import io.harness.delegate.task.stepstatus.StepStatus;
import io.harness.delegate.task.stepstatus.StepStatusTaskResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plugin.GitCloneStep;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PluginCreationRequest;
import io.harness.pms.contracts.plan.PluginCreationResponse;
import io.harness.pms.contracts.plan.PluginCreationResponseList;
import io.harness.pms.contracts.plan.PluginCreationResponseWrapper;
import io.harness.pms.contracts.plan.PluginDetails;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.plugin.ContainerStepExecutionResponseHelper;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;
import io.harness.tasks.ResponseData;

import com.google.protobuf.LazyStringArrayList;
import com.google.protobuf.ProtocolStringList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.groovy.util.Maps;
import org.junit.Before;
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
public class AwsSamDownloadManifestsStepHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private OutcomeService outcomeService;

  @Mock private ExecutionSweepingOutputService executionSweepingOutputService;

  @Mock private EngineExpressionService engineExpressionService;

  @Mock private GitClonePluginInfoProvider gitClonePluginInfoProvider;

  @Mock private DownloadManifestsCommonHelper downloadManifestsCommonHelper;
  @Mock private AwsSamStepHelper awsSamStepHelper;
  @Mock private ContainerStepExecutionResponseHelper containerStepExecutionResponseHelper;

  @Mock private DownloadAwsS3PluginInfoProvider downloadAwsS3PluginInfoProvider;

  @Mock private DownloadAwsS3Step downloadAwsS3Step;

  @Mock private DownloadHarnessStoreStep downloadHarnessStoreStep;

  @Mock private DownloadHarnessStorePluginInfoProvider downloadHarnessStorePluginInfoProvider;

  @Mock private GitCloneStep gitCloneStep;

  @Mock private CDFeatureFlagHelper cdFeatureFlagHelper;
  @InjectMocks @Spy private AwsSamDownloadManifestsStepHelper awsSamDownloadManifestsStepHelper;

  @Before
  public void setup() {}

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetPluginInfoList() {
    String stepJsonNode = "json";
    PluginCreationRequest pluginCreationRequest =
        PluginCreationRequest.newBuilder().setStepJsonNode(stepJsonNode).build();

    CdAbstractStepNode cdAbstractStepNode = mock(CdAbstractStepNode.class);

    Mockito.mockStatic(YamlUtils.class);
    when(YamlUtils.read(stepJsonNode, CdAbstractStepNode.class)).thenReturn(cdAbstractStepNode);
    when(YamlUtils.writeYamlString(any(Object.class))).thenReturn("yaml1");

    ManifestsOutcome manifestsOutcome = mock(ManifestsOutcome.class);
    doReturn(OptionalOutcome.builder().outcome(manifestsOutcome).build())
        .when(outcomeService)
        .resolveOptional(any(), any());

    ManifestOutcome valuesManifestOutcome = mock(ManifestOutcome.class);
    doReturn(Arrays.asList(valuesManifestOutcome)).when(manifestsOutcome).values();

    AwsSamDirectoryManifestOutcome awsSamDirectoryManifestOutcome = mock(AwsSamDirectoryManifestOutcome.class);
    doReturn(awsSamDirectoryManifestOutcome).when(awsSamStepHelper).getAwsSamDirectoryManifestOutcome(any());
    doReturn(GithubStore.builder().build()).when(awsSamDirectoryManifestOutcome).getStore();

    GitCloneStepInfo gitCloneStepInfo = mock(GitCloneStepInfo.class);
    doReturn(gitCloneStepInfo).when(downloadManifestsCommonHelper).getGitCloneStepInfoFromManifestOutcome(any());

    GitCloneStepNode gitCloneStepNode = mock(GitCloneStepNode.class);
    doReturn(gitCloneStepNode).when(downloadManifestsCommonHelper).getGitCloneStepNode(any(), any(), any());

    ValuesManifestOutcome valuesManifestOutcome1 = mock(ValuesManifestOutcome.class);
    doReturn(valuesManifestOutcome1).when(awsSamStepHelper).getAwsSamValuesManifestOutcome(any());
    doReturn(GitLabStore.builder().build()).when(valuesManifestOutcome1).getStore();

    PluginCreationResponseWrapper pluginCreationResponseWrapper = mock(PluginCreationResponseWrapper.class);
    doReturn(pluginCreationResponseWrapper).when(gitClonePluginInfoProvider).getPluginInfo(any(), any(), any());

    PluginCreationResponse pluginCreationResponse = mock(PluginCreationResponse.class);
    doReturn(pluginCreationResponse).when(pluginCreationResponseWrapper).getResponse();

    PluginDetails pluginDetails = mock(PluginDetails.class);
    doReturn(pluginDetails).when(pluginCreationResponse).getPluginDetails();

    doReturn(Arrays.asList(32)).when(pluginDetails).getPortUsedList();

    String accountId = "accountId";
    Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();

    PluginCreationResponseList pluginCreationResponseList =
        awsSamDownloadManifestsStepHelper.getPluginInfoList(pluginCreationRequest, new HashSet<>(), ambiance);
    assertThat(pluginCreationResponseList.getResponseList().size()).isEqualTo(2);
  }

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    String accountId = "accountId";
    Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();

    ManifestsOutcome manifestsOutcome = mock(ManifestsOutcome.class);
    doReturn(OptionalOutcome.builder().outcome(manifestsOutcome).build())
        .when(outcomeService)
        .resolveOptional(any(), any());

    ValuesManifestOutcome valuesManifestOutcome1 = ValuesManifestOutcome.builder().build();
    doReturn(valuesManifestOutcome1).when(awsSamStepHelper).getAwsSamValuesManifestOutcome(any());

    Map<String, ResponseData> responseDataMap = new HashMap<>();

    String valuesYamlPath = "yaml";
    HashMap<String, String> map = new HashMap<>();
    map.put(valuesYamlPath, "content");
    StepOutput stepOutput = StepMapOutput.builder().map(map).build();
    ResponseData responseData =
        StepStatusTaskResponseData.builder()
            .stepStatus(
                StepStatus.builder().output(stepOutput).stepExecutionStatus(StepExecutionStatus.SUCCESS).build())
            .build();
    responseDataMap.put("key", responseData);

    doReturn(valuesYamlPath).when(awsSamStepHelper).getValuesPathFromValuesManifestOutcome(any());
    String valuesYamlContent = "content";
    doReturn(valuesYamlContent).when(engineExpressionService).renderExpression(any(), any());
    doNothing().when(containerStepExecutionResponseHelper).deserializeResponse(any());

    StepResponse stepResponse = awsSamDownloadManifestsStepHelper.handleAsyncResponse(ambiance, responseDataMap);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
  }

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testExecuteAsyncAfterRbac() {
    String accountId = "accountId";
    Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();

    ManifestsOutcome manifestsOutcome = mock(ManifestsOutcome.class);
    doReturn(manifestsOutcome).when(downloadManifestsCommonHelper).fetchManifestsOutcome(ambiance);

    AwsSamDirectoryManifestOutcome awsSamDirectoryManifestOutcome =
        AwsSamDirectoryManifestOutcome.builder().store(GitStore.builder().build()).build();
    doReturn(awsSamDirectoryManifestOutcome).when(awsSamStepHelper).getAwsSamDirectoryManifestOutcome(any());

    GitCloneStepInfo gitCloneStepInfo = mock(GitCloneStepInfo.class);
    doReturn(gitCloneStepInfo).when(downloadManifestsCommonHelper).getGitCloneStepInfoFromManifestOutcome(any());

    StepElementParameters stepElementParameters = StepElementParameters.builder().build();
    doReturn(stepElementParameters).when(downloadManifestsCommonHelper).getGitStepElementParameters(any(), any());

    doReturn("iden").when(downloadManifestsCommonHelper).getGitCloneStepIdentifier(any());

    doReturn(ambiance).when(downloadManifestsCommonHelper).buildAmbiance(any(), any());

    AsyncExecutableResponse asyncExecutableResponse =
        AsyncExecutableResponse.newBuilder().addCallbackIds("1").addLogKeys("1").setStatus(Status.RUNNING).build();
    doReturn(asyncExecutableResponse).when(gitCloneStep).executeAsyncAfterRbac(any(), any(), any());

    ManifestOutcome valuesManifestOutcome = mock(ManifestOutcome.class);
    doReturn(Arrays.asList(valuesManifestOutcome)).when(manifestsOutcome).values();

    ValuesManifestOutcome valuesManifestOutcome1 =
        ValuesManifestOutcome.builder().store(BitbucketStore.builder().build()).build();
    doReturn(valuesManifestOutcome1).when(awsSamStepHelper).getAwsSamValuesManifestOutcome(any());

    AsyncExecutableResponse asyncExecutableResponse1 = awsSamDownloadManifestsStepHelper.executeAsyncAfterRbac(
        ambiance, StepInputPackage.builder().build(), gitCloneStep);

    assertThat(asyncExecutableResponse1.getCallbackIdsList().size()).isEqualTo(2);
    assertThat(asyncExecutableResponse1.getLogKeysList().size()).isEqualTo(2);
    assertThat(asyncExecutableResponse1.getStatus()).isEqualTo(Status.RUNNING);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetPluginInfoTestWhenValuesYamlAbsentWhenSamManifestInS3() throws IOException {
    String accountId = "accountId";
    Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();

    String jsonNode = "jsonNdod";
    PluginCreationRequest pluginCreationRequest = PluginCreationRequest.newBuilder().setStepJsonNode(jsonNode).build();
    CdAbstractStepNode cdAbstractStepNode = mock(CdAbstractStepNode.class);
    doReturn("identifier").when(cdAbstractStepNode).getIdentifier();
    doReturn("name").when(cdAbstractStepNode).getName();
    doReturn("uuid").when(cdAbstractStepNode).getUuid();

    Mockito.mockStatic(YamlUtils.class);
    when(YamlUtils.read(jsonNode, CdAbstractStepNode.class)).thenReturn(cdAbstractStepNode);
    when(YamlUtils.writeYamlString(any(Object.class))).thenReturn("yaml1");

    ManifestsOutcome manifestsOutcome = new ManifestsOutcome();
    OptionalOutcome optionalManifestsOutcome = OptionalOutcome.builder().found(true).outcome(manifestsOutcome).build();
    doReturn(optionalManifestsOutcome).when(outcomeService).resolveOptional(any(), any());

    doReturn(true).when(cdFeatureFlagHelper).isEnabled(any(), any());

    AwsSamDirectoryManifestOutcome awsSamDirectoryManifestOutcome = mock(AwsSamDirectoryManifestOutcome.class);
    doReturn(S3StoreConfig.builder().build()).when(awsSamDirectoryManifestOutcome).getStore();
    doReturn(awsSamDirectoryManifestOutcome).when(awsSamStepHelper).getAwsSamDirectoryManifestOutcome(any());

    DownloadAwsS3StepInfo downloadAwsS3StepInfo = mock(DownloadAwsS3StepInfo.class);
    doReturn(downloadAwsS3StepInfo).when(downloadManifestsCommonHelper).getAwsS3StepInfo(any(), any());

    DownloadAwsS3StepNode downloadAwsS3StepNode = mock(DownloadAwsS3StepNode.class);
    doReturn(downloadAwsS3StepNode).when(downloadManifestsCommonHelper).getAwsS3StepNode(any(), any(), any());

    List<Integer> portList = new ArrayList<>(Arrays.asList(1));
    PluginDetails pluginDetails = mock(PluginDetails.class);
    doReturn(portList).when(pluginDetails).getPortUsedList();
    PluginCreationResponse pluginCreationResponse =
        PluginCreationResponse.newBuilder().setPluginDetails(pluginDetails).build();
    PluginCreationResponseWrapper pluginCreationResponseWrapper =
        PluginCreationResponseWrapper.newBuilder().setResponse(pluginCreationResponse).build();
    doReturn(pluginCreationResponseWrapper).when(downloadAwsS3PluginInfoProvider).getPluginInfo(any(), any(), any());

    doReturn(null).when(awsSamStepHelper).getAwsSamValuesManifestOutcome(any());
    doReturn(manifestsOutcome).when(downloadManifestsCommonHelper).fetchManifestsOutcome(any());

    PluginCreationResponseList pluginCreationResponseList =
        awsSamDownloadManifestsStepHelper.getPluginInfoList(pluginCreationRequest, new HashSet<Integer>(), ambiance);
    assertThat(pluginCreationResponseList.getResponseList().size()).isEqualTo(1);
    assertThat(
        pluginCreationResponseList.getResponseList().get(0).getResponse().getPluginDetails().getPortUsedList().size())
        .isEqualTo(1);
    assertThat(
        pluginCreationResponseList.getResponseList().get(0).getResponse().getPluginDetails().getPortUsedList().get(0))
        .isEqualTo(1);
  }

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void executeAsyncAfterRbacWhenValuesYamlPresentInAwsS3Test() {
    String accountId = "accountId";
    Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().build();

    ManifestsOutcome manifestsOutcome = mock(ManifestsOutcome.class);
    doReturn(manifestsOutcome).when(downloadManifestsCommonHelper).fetchManifestsOutcome(any());

    doReturn(true).when(cdFeatureFlagHelper).isEnabled(any(), any());

    AwsSamDirectoryManifestOutcome awsSamDirectoryManifestOutcome = mock(AwsSamDirectoryManifestOutcome.class);
    doReturn(awsSamDirectoryManifestOutcome).when(awsSamStepHelper).getAwsSamDirectoryManifestOutcome(any());

    doReturn("path").when(awsSamStepHelper).getValuesPathFromValuesManifestOutcome(any());

    DownloadAwsS3StepInfo downloadAwsS3StepInfo = mock(DownloadAwsS3StepInfo.class);
    doReturn(downloadAwsS3StepInfo).when(downloadManifestsCommonHelper).getAwsS3StepInfo(any(), any());

    doReturn(downloadAwsS3StepInfo)
        .when(downloadManifestsCommonHelper)
        .getAwsS3StepInfoWithOutputFilePathContents(any(), any(), any());

    StepElementParameters stepElementParameters1 = mock(StepElementParameters.class);
    doReturn(stepElementParameters1)
        .when(downloadManifestsCommonHelper)
        .getDownloadS3StepElementParameters(any(), any());

    String identifier = "identifier";
    doReturn(identifier).when(downloadManifestsCommonHelper).getDownloadS3StepIdentifier(any());

    Ambiance ambiance1 = mock(Ambiance.class);
    doReturn(ambiance1).when(downloadManifestsCommonHelper).buildAmbiance(any(), any());

    doReturn(S3StoreConfig.builder().build()).when(awsSamDirectoryManifestOutcome).getStore();

    AsyncExecutableResponse asyncExecutableResponse = mock(AsyncExecutableResponse.class);
    doReturn(Status.SUCCEEDED).when(asyncExecutableResponse).getStatus();
    doReturn(asyncExecutableResponse).when(downloadAwsS3Step).executeAsyncAfterRbac(any(), any(), any());

    ProtocolStringList callbackIdList = new LazyStringArrayList();
    callbackIdList.add("1");

    ProtocolStringList logKeysList = new LazyStringArrayList();
    logKeysList.add("2");

    doReturn(callbackIdList).when(asyncExecutableResponse).getCallbackIdsList();
    doReturn(logKeysList).when(asyncExecutableResponse).getLogKeysList();

    ValuesManifestOutcome valuesManifestOutcome = mock(ValuesManifestOutcome.class);
    doReturn(valuesManifestOutcome).when(awsSamStepHelper).getAwsSamValuesManifestOutcome(any());
    doReturn(S3StoreConfig.builder().build()).when(valuesManifestOutcome).getStore();

    doReturn("path").when(awsSamStepHelper).getValuesPathFromValuesManifestOutcome(any());

    AsyncExecutableResponse asyncExecutableResponse1 = awsSamDownloadManifestsStepHelper.executeAsyncAfterRbac(
        ambiance, StepInputPackage.builder().build(), gitCloneStep);
    assertThat(asyncExecutableResponse1.getCallbackIdsList().size()).isEqualTo(2);
    assertThat(asyncExecutableResponse1.getLogKeysList().size()).isEqualTo(2);
    assertThat(asyncExecutableResponse1.getStatus()).isEqualTo(Status.SUCCEEDED);
  }

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void executeAsyncAfterRbacWhenValuesYamlAbsentAndSamManifestPresentInAwsS3Test() {
    String accountId = "accountId";
    Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().build();

    doReturn(true).when(cdFeatureFlagHelper).isEnabled(any(), any());

    ManifestsOutcome manifestsOutcome = mock(ManifestsOutcome.class);
    doReturn(manifestsOutcome).when(downloadManifestsCommonHelper).fetchManifestsOutcome(any());

    AwsSamDirectoryManifestOutcome awsSamDirectoryManifestOutcome = mock(AwsSamDirectoryManifestOutcome.class);
    doReturn(awsSamDirectoryManifestOutcome).when(awsSamStepHelper).getAwsSamDirectoryManifestOutcome(any());

    DownloadAwsS3StepInfo downloadAwsS3StepInfo = mock(DownloadAwsS3StepInfo.class);
    doReturn(downloadAwsS3StepInfo).when(downloadManifestsCommonHelper).getAwsS3StepInfo(any(), any());

    StepElementParameters stepElementParameters1 = mock(StepElementParameters.class);
    doReturn(stepElementParameters1).when(downloadManifestsCommonHelper).getGitStepElementParameters(any(), any());

    String identifier = "identifier";
    doReturn(identifier).when(downloadManifestsCommonHelper).getDownloadS3StepIdentifier(any());

    Ambiance ambiance1 = mock(Ambiance.class);
    doReturn(ambiance1).when(downloadManifestsCommonHelper).buildAmbiance(any(), any());

    AsyncExecutableResponse asyncExecutableResponse = mock(AsyncExecutableResponse.class);
    doReturn(Status.SUCCEEDED).when(asyncExecutableResponse).getStatus();
    doReturn(asyncExecutableResponse).when(downloadAwsS3Step).executeAsyncAfterRbac(any(), any(), any());

    doReturn(S3StoreConfig.builder().build()).when(awsSamDirectoryManifestOutcome).getStore();

    ProtocolStringList callbackIdList = new LazyStringArrayList();
    callbackIdList.add("1");

    ProtocolStringList logKeysList = new LazyStringArrayList();
    logKeysList.add("2");

    doReturn(callbackIdList).when(asyncExecutableResponse).getCallbackIdsList();
    doReturn(logKeysList).when(asyncExecutableResponse).getLogKeysList();

    ValuesManifestOutcome valuesManifestOutcome = mock(ValuesManifestOutcome.class);
    doReturn(null).when(awsSamStepHelper).getAwsSamValuesManifestOutcome(any());

    doReturn(S3StoreConfig.builder().build()).when(valuesManifestOutcome).getStore();

    doReturn("path").when(awsSamStepHelper).getValuesPathFromValuesManifestOutcome(any());

    AsyncExecutableResponse asyncExecutableResponse1 = awsSamDownloadManifestsStepHelper.executeAsyncAfterRbac(
        ambiance, StepInputPackage.builder().build(), gitCloneStep);
    assertThat(asyncExecutableResponse1.getCallbackIdsList().size()).isEqualTo(1);
    assertThat(asyncExecutableResponse1.getLogKeysList().size()).isEqualTo(1);
    assertThat(asyncExecutableResponse1.getStatus()).isEqualTo(Status.SUCCEEDED);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetValuesManifestContent() {
    String valuesYamlPath = "harness/path/to/values.yaml";
    String valuesManifestContent = awsSamDownloadManifestsStepHelper.getValuesManifestContent(
        StepMapOutput.builder().map(Maps.of(valuesYamlPath, "dmFsdWVzIHlhbW wgY29udGVudA--")).build(), valuesYamlPath);

    assertThat(valuesManifestContent).isEqualTo("values yaml content");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetValuesManifestContentEmpty() {
    String valuesYamlPath = "harness/path/to/values.yaml";
    String valuesManifestContent = awsSamDownloadManifestsStepHelper.getValuesManifestContent(
        StepMapOutput.builder().map(Maps.of(valuesYamlPath, null)).build(), valuesYamlPath);

    assertThat(valuesManifestContent).isEmpty();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetValuesManifestContentWithIllegalBase64Character() {
    String valuesYamlPath = "harness/path/to/values.yaml";
    assertThatThrownBy(
        ()
            -> awsSamDownloadManifestsStepHelper.getValuesManifestContent(
                StepMapOutput.builder().map(Maps.of(valuesYamlPath, "$dmFsdWVzIHlhbWwgY29udGVudA--")).build(),
                valuesYamlPath))
        .hasMessage("Unable to fetch values YAML, valuesYamlPath: harness/path/to/values.yaml")
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetPluginInfoTestWhenValuesYamlAbsentWhenSamManifestInHarnessStore() throws IOException {
    String accountId = "accountId";
    Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();

    String jsonNode = "jsonNdod";
    PluginCreationRequest pluginCreationRequest = PluginCreationRequest.newBuilder().setStepJsonNode(jsonNode).build();
    CdAbstractStepNode cdAbstractStepNode = mock(CdAbstractStepNode.class);
    doReturn("identifier").when(cdAbstractStepNode).getIdentifier();
    doReturn("name").when(cdAbstractStepNode).getName();
    doReturn("uuid").when(cdAbstractStepNode).getUuid();

    Mockito.mockStatic(YamlUtils.class);
    when(YamlUtils.read(jsonNode, CdAbstractStepNode.class)).thenReturn(cdAbstractStepNode);
    when(YamlUtils.writeYamlString(any(Object.class))).thenReturn("yaml1");

    ManifestsOutcome manifestsOutcome = new ManifestsOutcome();
    OptionalOutcome optionalManifestsOutcome = OptionalOutcome.builder().found(true).outcome(manifestsOutcome).build();
    doReturn(optionalManifestsOutcome).when(outcomeService).resolveOptional(any(), any());

    doReturn(true).when(cdFeatureFlagHelper).isEnabled(any(), any());

    AwsSamDirectoryManifestOutcome awsSamDirectoryManifestOutcome = mock(AwsSamDirectoryManifestOutcome.class);
    doReturn(HarnessStore.builder().build()).when(awsSamDirectoryManifestOutcome).getStore();
    doReturn(awsSamDirectoryManifestOutcome).when(awsSamStepHelper).getAwsSamDirectoryManifestOutcome(any());

    DownloadHarnessStoreStepInfo downloadHarnessStoreStepInfo = mock(DownloadHarnessStoreStepInfo.class);
    doReturn(downloadHarnessStoreStepInfo)
        .when(downloadManifestsCommonHelper)
        .getDownloadHarnessStoreStepInfo(any(), any());

    DownloadHarnessStoreStepNode downloadHarnessStoreStepNode = mock(DownloadHarnessStoreStepNode.class);
    doReturn(downloadHarnessStoreStepNode)
        .when(downloadManifestsCommonHelper)
        .getDownloadHarnessStoreStepNode(any(), any(), any());

    List<Integer> portList = new ArrayList<>(Arrays.asList(1));
    PluginDetails pluginDetails = mock(PluginDetails.class);
    doReturn(portList).when(pluginDetails).getPortUsedList();
    PluginCreationResponse pluginCreationResponse =
        PluginCreationResponse.newBuilder().setPluginDetails(pluginDetails).build();
    PluginCreationResponseWrapper pluginCreationResponseWrapper =
        PluginCreationResponseWrapper.newBuilder().setResponse(pluginCreationResponse).build();
    doReturn(pluginCreationResponseWrapper)
        .when(downloadHarnessStorePluginInfoProvider)
        .getPluginInfo(any(), any(), any());

    doReturn(null).when(awsSamStepHelper).getAwsSamValuesManifestOutcome(any());
    doReturn(manifestsOutcome).when(downloadManifestsCommonHelper).fetchManifestsOutcome(any());

    PluginCreationResponseList pluginCreationResponseList =
        awsSamDownloadManifestsStepHelper.getPluginInfoList(pluginCreationRequest, new HashSet<Integer>(), ambiance);
    assertThat(pluginCreationResponseList.getResponseList().size()).isEqualTo(1);
    assertThat(
        pluginCreationResponseList.getResponseList().get(0).getResponse().getPluginDetails().getPortUsedList().size())
        .isEqualTo(1);
    assertThat(
        pluginCreationResponseList.getResponseList().get(0).getResponse().getPluginDetails().getPortUsedList().get(0))
        .isEqualTo(1);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetPluginInfoTestWhenValuesYamlPresentInHarnessStore() throws IOException {
    String accountId = "accountId";
    Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();

    String jsonNode = "jsonNdod";
    PluginCreationRequest pluginCreationRequest = PluginCreationRequest.newBuilder().setStepJsonNode(jsonNode).build();
    CdAbstractStepNode cdAbstractStepNode = mock(CdAbstractStepNode.class);
    doReturn("identifier").when(cdAbstractStepNode).getIdentifier();
    doReturn("name").when(cdAbstractStepNode).getName();
    doReturn("uuid").when(cdAbstractStepNode).getUuid();

    Mockito.mockStatic(YamlUtils.class);
    when(YamlUtils.read(jsonNode, CdAbstractStepNode.class)).thenReturn(cdAbstractStepNode);
    when(YamlUtils.writeYamlString(any(Object.class))).thenReturn("yaml1");
    doReturn(true).when(cdFeatureFlagHelper).isEnabled(any(), any());

    ManifestsOutcome manifestsOutcome = new ManifestsOutcome();
    OptionalOutcome optionalManifestsOutcome = OptionalOutcome.builder().found(true).outcome(manifestsOutcome).build();
    doReturn(optionalManifestsOutcome).when(outcomeService).resolveOptional(any(), any());

    AwsSamDirectoryManifestOutcome awsSamDirectoryManifestOutcome = mock(AwsSamDirectoryManifestOutcome.class);
    doReturn(awsSamDirectoryManifestOutcome).when(awsSamStepHelper).getAwsSamDirectoryManifestOutcome(any());

    doReturn(HarnessStore.builder().build()).when(awsSamDirectoryManifestOutcome).getStore();

    DownloadHarnessStoreStepInfo downloadHarnessStoreStepInfo = mock(DownloadHarnessStoreStepInfo.class);
    doReturn(downloadHarnessStoreStepInfo)
        .when(downloadManifestsCommonHelper)
        .getDownloadHarnessStoreStepInfo(any(), any());

    DownloadHarnessStoreStepNode downloadHarnessStoreStepNode = mock(DownloadHarnessStoreStepNode.class);
    doReturn(downloadHarnessStoreStepNode)
        .when(downloadManifestsCommonHelper)
        .getDownloadHarnessStoreStepNode(any(), any(), any());

    doReturn(downloadHarnessStoreStepInfo)
        .when(downloadManifestsCommonHelper)
        .getDownloadHarnessStoreStepInfoWithOutputFilePathContents(any(), any(), any());

    List<Integer> portList = new ArrayList<>(Arrays.asList(1));
    PluginDetails pluginDetails = mock(PluginDetails.class);
    doReturn(portList).when(pluginDetails).getPortUsedList();
    PluginCreationResponse pluginCreationResponse =
        PluginCreationResponse.newBuilder().setPluginDetails(pluginDetails).build();
    PluginCreationResponseWrapper pluginCreationResponseWrapper =
        PluginCreationResponseWrapper.newBuilder().setResponse(pluginCreationResponse).build();
    doReturn(pluginCreationResponseWrapper)
        .when(downloadHarnessStorePluginInfoProvider)
        .getPluginInfo(any(), any(), any());

    ValuesManifestOutcome valuesManifestOutcome = mock(ValuesManifestOutcome.class);
    doReturn(HarnessStore.builder().build()).when(valuesManifestOutcome).getStore();
    doReturn("path").when(awsSamStepHelper).getValuesPathFromValuesManifestOutcome(any());
    doReturn(valuesManifestOutcome).when(awsSamStepHelper).getAwsSamValuesManifestOutcome(any());
    doReturn(manifestsOutcome).when(downloadManifestsCommonHelper).fetchManifestsOutcome(any());

    PluginCreationResponseList pluginCreationResponseList =
        awsSamDownloadManifestsStepHelper.getPluginInfoList(pluginCreationRequest, new HashSet<Integer>(), ambiance);
    assertThat(pluginCreationResponseList.getResponseList().size()).isEqualTo(2);
    assertThat(
        pluginCreationResponseList.getResponseList().get(0).getResponse().getPluginDetails().getPortUsedList().size())
        .isEqualTo(1);
    assertThat(
        pluginCreationResponseList.getResponseList().get(1).getResponse().getPluginDetails().getPortUsedList().size())
        .isEqualTo(1);
  }

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void executeAsyncAfterRbacWhenValuesYamlPresentInHarnessStoreTest() {
    String accountId = "accountId";
    Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().build();

    ManifestsOutcome manifestsOutcome = mock(ManifestsOutcome.class);
    doReturn(manifestsOutcome).when(downloadManifestsCommonHelper).fetchManifestsOutcome(any());

    doReturn(true).when(cdFeatureFlagHelper).isEnabled(any(), any());

    AwsSamDirectoryManifestOutcome awsSamDirectoryManifestOutcome = mock(AwsSamDirectoryManifestOutcome.class);
    doReturn(awsSamDirectoryManifestOutcome).when(awsSamStepHelper).getAwsSamDirectoryManifestOutcome(any());

    doReturn("path").when(awsSamStepHelper).getValuesPathFromValuesManifestOutcome(any());

    DownloadHarnessStoreStepInfo downloadHarnessStoreStepInfo = mock(DownloadHarnessStoreStepInfo.class);
    doReturn(downloadHarnessStoreStepInfo)
        .when(downloadManifestsCommonHelper)
        .getDownloadHarnessStoreStepInfo(any(), any());

    doReturn(downloadHarnessStoreStepInfo)
        .when(downloadManifestsCommonHelper)
        .getDownloadHarnessStoreStepInfoWithOutputFilePathContents(any(), any(), any());

    StepElementParameters stepElementParameters1 = mock(StepElementParameters.class);
    doReturn(stepElementParameters1)
        .when(downloadManifestsCommonHelper)
        .getDownloadHarnessStoreStepElementParameters(any(), any());

    String identifier = "identifier";
    doReturn(identifier).when(downloadManifestsCommonHelper).getDownloadHarnessStoreStepIdentifier(any());

    Ambiance ambiance1 = mock(Ambiance.class);
    doReturn(ambiance1).when(downloadManifestsCommonHelper).buildAmbiance(any(), any());

    doReturn(HarnessStore.builder().build()).when(awsSamDirectoryManifestOutcome).getStore();

    AsyncExecutableResponse asyncExecutableResponse = mock(AsyncExecutableResponse.class);
    doReturn(Status.SUCCEEDED).when(asyncExecutableResponse).getStatus();
    doReturn(asyncExecutableResponse).when(downloadHarnessStoreStep).executeAsyncAfterRbac(any(), any(), any());

    ProtocolStringList callbackIdList = new LazyStringArrayList();
    callbackIdList.add("1");

    ProtocolStringList logKeysList = new LazyStringArrayList();
    logKeysList.add("2");

    doReturn(callbackIdList).when(asyncExecutableResponse).getCallbackIdsList();
    doReturn(logKeysList).when(asyncExecutableResponse).getLogKeysList();

    ValuesManifestOutcome valuesManifestOutcome = mock(ValuesManifestOutcome.class);
    doReturn(valuesManifestOutcome).when(awsSamStepHelper).getAwsSamValuesManifestOutcome(any());
    doReturn(HarnessStore.builder().build()).when(valuesManifestOutcome).getStore();

    doReturn("path").when(awsSamStepHelper).getValuesPathFromValuesManifestOutcome(any());

    AsyncExecutableResponse asyncExecutableResponse1 = awsSamDownloadManifestsStepHelper.executeAsyncAfterRbac(
        ambiance, StepInputPackage.builder().build(), gitCloneStep);
    assertThat(asyncExecutableResponse1.getCallbackIdsList().size()).isEqualTo(2);
    assertThat(asyncExecutableResponse1.getLogKeysList().size()).isEqualTo(2);
    assertThat(asyncExecutableResponse1.getStatus()).isEqualTo(Status.SUCCEEDED);
  }

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void executeAsyncAfterRbacWhenValuesYamlAbsentAndServerlessYamlPresentInHarnessStoreTest() {
    String accountId = "accountId";
    Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().build();

    doReturn(true).when(cdFeatureFlagHelper).isEnabled(any(), any());

    ManifestsOutcome manifestsOutcome = mock(ManifestsOutcome.class);
    doReturn(manifestsOutcome).when(downloadManifestsCommonHelper).fetchManifestsOutcome(any());

    AwsSamDirectoryManifestOutcome awsSamDirectoryManifestOutcome = mock(AwsSamDirectoryManifestOutcome.class);
    doReturn(awsSamDirectoryManifestOutcome).when(awsSamStepHelper).getAwsSamDirectoryManifestOutcome(any());

    DownloadHarnessStoreStepInfo downloadHarnessStoreStepInfo = mock(DownloadHarnessStoreStepInfo.class);
    doReturn(downloadHarnessStoreStepInfo)
        .when(downloadManifestsCommonHelper)
        .getDownloadHarnessStoreStepInfo(any(), any());

    StepElementParameters stepElementParameters1 = mock(StepElementParameters.class);
    doReturn(stepElementParameters1).when(downloadManifestsCommonHelper).getGitStepElementParameters(any(), any());

    String identifier = "identifier";
    doReturn(identifier).when(downloadManifestsCommonHelper).getDownloadHarnessStoreStepIdentifier(any());

    Ambiance ambiance1 = mock(Ambiance.class);
    doReturn(ambiance1).when(downloadManifestsCommonHelper).buildAmbiance(any(), any());

    AsyncExecutableResponse asyncExecutableResponse = mock(AsyncExecutableResponse.class);
    doReturn(Status.SUCCEEDED).when(asyncExecutableResponse).getStatus();
    doReturn(asyncExecutableResponse).when(downloadHarnessStoreStep).executeAsyncAfterRbac(any(), any(), any());

    doReturn(HarnessStore.builder().build()).when(awsSamDirectoryManifestOutcome).getStore();

    ProtocolStringList callbackIdList = new LazyStringArrayList();
    callbackIdList.add("1");

    ProtocolStringList logKeysList = new LazyStringArrayList();
    logKeysList.add("2");

    doReturn(callbackIdList).when(asyncExecutableResponse).getCallbackIdsList();
    doReturn(logKeysList).when(asyncExecutableResponse).getLogKeysList();

    ValuesManifestOutcome valuesManifestOutcome = mock(ValuesManifestOutcome.class);
    doReturn(null).when(awsSamStepHelper).getAwsSamValuesManifestOutcome(any());

    doReturn(HarnessStore.builder().build()).when(valuesManifestOutcome).getStore();

    doReturn("path").when(awsSamStepHelper).getValuesPathFromValuesManifestOutcome(any());

    AsyncExecutableResponse asyncExecutableResponse1 = awsSamDownloadManifestsStepHelper.executeAsyncAfterRbac(
        ambiance, StepInputPackage.builder().build(), gitCloneStep);
    assertThat(asyncExecutableResponse1.getCallbackIdsList().size()).isEqualTo(1);
    assertThat(asyncExecutableResponse1.getLogKeysList().size()).isEqualTo(1);
    assertThat(asyncExecutableResponse1.getStatus()).isEqualTo(Status.SUCCEEDED);
  }
}