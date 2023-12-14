/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.aws.sam;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.containerStepGroup.DownloadAwsS3StepInfo;
import io.harness.cdng.containerStepGroup.DownloadAwsS3StepNode;
import io.harness.cdng.containerStepGroup.DownloadHarnessStoreStepInfo;
import io.harness.cdng.containerStepGroup.DownloadHarnessStoreStepNode;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.cdng.manifest.yaml.ServerlessAwsLambdaManifestOutcome;
import io.harness.cdng.manifest.yaml.harness.HarnessStore;
import io.harness.cdng.pipeline.steps.CdAbstractStepNode;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.timeout.Timeout;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@OwnedBy(CDP)
public class DownloadManifestsCommonHelperTest extends CategoryTest {
  @Mock private OutcomeService outcomeService;
  @InjectMocks @Spy DownloadManifestsCommonHelper downloadManifestsCommonHelper;

  protected final Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", "accountid").build();

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetDownloadS3StepElementParameters() {
    String identifier = "identifier";
    ManifestOutcome manifestOutcome = ServerlessAwsLambdaManifestOutcome.builder().identifier(identifier).build();
    DownloadAwsS3StepInfo downloadAwsS3StepInfo = DownloadAwsS3StepInfo.infoBuilder().build();
    StepElementParameters stepElementParameters =
        downloadManifestsCommonHelper.getDownloadS3StepElementParameters(manifestOutcome, downloadAwsS3StepInfo);
    assertThat(stepElementParameters.getIdentifier()).isEqualTo(identifier);
    assertThat(stepElementParameters.getName()).isEqualTo(identifier);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetDownloadS3StepIdentifier() {
    String identifier = "identifier";
    ManifestOutcome manifestOutcome = ServerlessAwsLambdaManifestOutcome.builder().identifier(identifier).build();
    assertThat(downloadManifestsCommonHelper.getDownloadS3StepIdentifier(manifestOutcome)).isEqualTo(identifier);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetAwsS3StepInfoWithOutputFilePathContents() {
    String identifier = "identifier";
    ManifestOutcome manifestOutcome = ServerlessAwsLambdaManifestOutcome.builder().identifier(identifier).build();

    String valuesPath = "valuesPath";

    String connectorRef = "ref";
    String bucketName = "bucket";
    String region = "region";
    List<String> paths = Arrays.asList("path");
    S3StoreConfig s3StoreConfig = S3StoreConfig.builder()
                                      .connectorRef(ParameterField.createValueField(connectorRef))
                                      .bucketName(ParameterField.createValueField(bucketName))
                                      .region(ParameterField.createValueField(region))
                                      .paths(ParameterField.createValueField(paths))
                                      .build();

    DownloadAwsS3StepInfo downloadAwsS3StepInfo =
        downloadManifestsCommonHelper.getAwsS3StepInfoWithOutputFilePathContents(
            manifestOutcome, s3StoreConfig, valuesPath);
    assertThat(downloadAwsS3StepInfo.getConnectorRef().getValue()).isEqualTo(connectorRef);
    assertThat(downloadAwsS3StepInfo.getBucketName().getValue()).isEqualTo(bucketName);
    assertThat(downloadAwsS3StepInfo.getRegion().getValue()).isEqualTo(region);
    assertThat(downloadAwsS3StepInfo.getPaths().getValue()).isEqualTo(paths);
    assertThat(downloadAwsS3StepInfo.getDownloadPath().getValue())
        .isEqualTo("/harness/" + manifestOutcome.getIdentifier());
    assertThat(downloadAwsS3StepInfo.getOutputFilePathsContent().getValue())
        .isEqualTo(Collections.singletonList(valuesPath));
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetAwsS3StepInfo() {
    String identifier = "identifier";
    ManifestOutcome manifestOutcome = ServerlessAwsLambdaManifestOutcome.builder().identifier(identifier).build();

    String connectorRef = "ref";
    String bucketName = "bucket";
    String region = "region";
    List<String> paths = Arrays.asList("path");
    S3StoreConfig s3StoreConfig = S3StoreConfig.builder()
                                      .connectorRef(ParameterField.createValueField(connectorRef))
                                      .bucketName(ParameterField.createValueField(bucketName))
                                      .region(ParameterField.createValueField(region))
                                      .paths(ParameterField.createValueField(paths))
                                      .build();

    DownloadAwsS3StepInfo downloadAwsS3StepInfo =
        downloadManifestsCommonHelper.getAwsS3StepInfo(manifestOutcome, s3StoreConfig);
    assertThat(downloadAwsS3StepInfo.getConnectorRef().getValue()).isEqualTo(connectorRef);
    assertThat(downloadAwsS3StepInfo.getBucketName().getValue()).isEqualTo(bucketName);
    assertThat(downloadAwsS3StepInfo.getRegion().getValue()).isEqualTo(region);
    assertThat(downloadAwsS3StepInfo.getPaths().getValue()).isEqualTo(paths);
    assertThat(downloadAwsS3StepInfo.getDownloadPath().getValue())
        .isEqualTo("/harness/" + manifestOutcome.getIdentifier());
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetAwsS3StepNode() {
    String identifier = "identifier";
    ManifestOutcome manifestOutcome = ServerlessAwsLambdaManifestOutcome.builder().identifier(identifier).build();
    DownloadAwsS3StepInfo downloadAwsS3StepInfo = DownloadAwsS3StepInfo.infoBuilder().build();

    CdAbstractStepNode cdAbstractStepNode = mock(CdAbstractStepNode.class);
    ParameterField<List<FailureStrategyConfig>> failureStrategies =
        ParameterField.createValueField(Arrays.asList(FailureStrategyConfig.builder().build()));
    doReturn(failureStrategies).when(cdAbstractStepNode).getFailureStrategies();

    ParameterField<Timeout> timeout = ParameterField.createValueField(Timeout.builder().build());
    doReturn(timeout).when(cdAbstractStepNode).getTimeout();

    DownloadAwsS3StepNode downloadAwsS3StepNode =
        downloadManifestsCommonHelper.getAwsS3StepNode(cdAbstractStepNode, manifestOutcome, downloadAwsS3StepInfo);
    assertThat(downloadAwsS3StepNode.getIdentifier()).isEqualTo(identifier);
    assertThat(downloadAwsS3StepNode.getName()).isEqualTo(identifier);
    assertThat(downloadAwsS3StepNode.getUuid()).isEqualTo(identifier);
    assertThat(downloadAwsS3StepNode.getDownloadAwsS3StepInfo()).isEqualTo(downloadAwsS3StepInfo);
    assertThat(downloadAwsS3StepNode.getFailureStrategies()).isEqualTo(failureStrategies);
    assertThat(downloadAwsS3StepNode.getTimeout()).isEqualTo(timeout);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetDownloadHarnessStoreStepElementParameters() {
    String identifier = "identifier";
    ManifestOutcome manifestOutcome = ServerlessAwsLambdaManifestOutcome.builder().identifier(identifier).build();
    DownloadHarnessStoreStepInfo downloadHarnessStoreStepInfo = DownloadHarnessStoreStepInfo.infoBuilder().build();
    StepElementParameters stepElementParameters =
        downloadManifestsCommonHelper.getDownloadHarnessStoreStepElementParameters(
            manifestOutcome, downloadHarnessStoreStepInfo);
    assertThat(stepElementParameters.getIdentifier()).isEqualTo(identifier);
    assertThat(stepElementParameters.getName()).isEqualTo(identifier);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetDownloadHarnessStoreStepIdentifier() {
    String identifier = "identifier";
    ManifestOutcome manifestOutcome = ServerlessAwsLambdaManifestOutcome.builder().identifier(identifier).build();
    assertThat(downloadManifestsCommonHelper.getDownloadHarnessStoreStepIdentifier(manifestOutcome))
        .isEqualTo(identifier);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetDownloadHarnessStoreStepInfoWithOutputFilePathContents() {
    String identifier = "identifier";
    ManifestOutcome manifestOutcome = ServerlessAwsLambdaManifestOutcome.builder().identifier(identifier).build();

    String valuesPath = "valuesPath";

    List<String> paths = Arrays.asList("path");
    HarnessStore harnessStore = HarnessStore.builder().files(ParameterField.createValueField(paths)).build();

    DownloadHarnessStoreStepInfo downloadHarnessStoreStepInfo =
        downloadManifestsCommonHelper.getDownloadHarnessStoreStepInfoWithOutputFilePathContents(
            manifestOutcome, harnessStore, valuesPath);
    assertThat(downloadHarnessStoreStepInfo.getFiles().getValue()).isEqualTo(paths);
    assertThat(downloadHarnessStoreStepInfo.getDownloadPath().getValue())
        .isEqualTo("/harness/" + manifestOutcome.getIdentifier());
    assertThat(downloadHarnessStoreStepInfo.getOutputFilePathsContent().getValue())
        .isEqualTo(Collections.singletonList(valuesPath));
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetDownloadHarnessStoreStepInfo() {
    String identifier = "identifier";
    ManifestOutcome manifestOutcome = ServerlessAwsLambdaManifestOutcome.builder().identifier(identifier).build();

    List<String> paths = Arrays.asList("path");
    HarnessStore harnessStore = HarnessStore.builder().files(ParameterField.createValueField(paths)).build();

    DownloadHarnessStoreStepInfo downloadHarnessStoreStepInfo =
        downloadManifestsCommonHelper.getDownloadHarnessStoreStepInfo(manifestOutcome, harnessStore);
    assertThat(downloadHarnessStoreStepInfo.getFiles().getValue()).isEqualTo(paths);
    assertThat(downloadHarnessStoreStepInfo.getDownloadPath().getValue())
        .isEqualTo("/harness/" + manifestOutcome.getIdentifier());
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetDownloadHarnessStoreStepNode() {
    String identifier = "identifier";
    ManifestOutcome manifestOutcome = ServerlessAwsLambdaManifestOutcome.builder().identifier(identifier).build();
    DownloadHarnessStoreStepInfo downloadHarnessStoreStepInfo = DownloadHarnessStoreStepInfo.infoBuilder().build();

    CdAbstractStepNode cdAbstractStepNode = mock(CdAbstractStepNode.class);
    ParameterField<List<FailureStrategyConfig>> failureStrategies =
        ParameterField.createValueField(Arrays.asList(FailureStrategyConfig.builder().build()));
    doReturn(failureStrategies).when(cdAbstractStepNode).getFailureStrategies();

    ParameterField<Timeout> timeout = ParameterField.createValueField(Timeout.builder().build());
    doReturn(timeout).when(cdAbstractStepNode).getTimeout();

    DownloadHarnessStoreStepNode downloadHarnessStoreStepNode =
        downloadManifestsCommonHelper.getDownloadHarnessStoreStepNode(
            cdAbstractStepNode, manifestOutcome, downloadHarnessStoreStepInfo);
    assertThat(downloadHarnessStoreStepNode.getIdentifier()).isEqualTo(identifier);
    assertThat(downloadHarnessStoreStepNode.getName()).isEqualTo(identifier);
    assertThat(downloadHarnessStoreStepNode.getUuid()).isEqualTo(identifier);
    assertThat(downloadHarnessStoreStepNode.getDownloadHarnessStoreStepInfo()).isEqualTo(downloadHarnessStoreStepInfo);
    assertThat(downloadHarnessStoreStepNode.getFailureStrategies()).isEqualTo(failureStrategies);
    assertThat(downloadHarnessStoreStepNode.getTimeout()).isEqualTo(timeout);
  }
}
