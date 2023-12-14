/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.plugininfoproviders;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.aws.asg.AsgBlueGreenDeployStepParameters;
import io.harness.cdng.expressions.CDExpressionResolver;
import io.harness.cdng.manifest.steps.outcome.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.ArtifactoryStoreConfig;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.cdng.manifest.yaml.ServerlessAwsLambdaManifestOutcome;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.cdng.manifest.yaml.harness.HarnessStore;
import io.harness.cdng.serverless.ServerlessEntityHelper;
import io.harness.cdng.serverless.container.steps.ServerlessAwsLambdaDeployV2StepParameters;
import io.harness.cdng.serverless.container.steps.ServerlessAwsLambdaPackageV2StepParameters;
import io.harness.cdng.serverless.container.steps.ServerlessAwsLambdaPrepareRollbackV2StepParameters;
import io.harness.cdng.serverless.container.steps.ServerlessAwsLambdaRollbackV2StepParameters;
import io.harness.connector.services.ConnectorService;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import com.google.inject.name.Named;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
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
public class ServerlessV2PluginInfoProviderHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private CDExpressionResolver cdExpressionResolver;
  @Mock private OutcomeService outcomeService;

  @Mock private ServerlessEntityHelper serverlessEntityHelper;

  @Named(DEFAULT_CONNECTOR_SERVICE) @Mock private ConnectorService connectorService;

  @Mock PluginInfoProviderUtils pluginInfoProviderUtils;
  private static final String PLUGIN_PATH_PREFIX = "harness";

  private static String NG_SECRET_MANAGER = "ngSecretManager";
  @InjectMocks @Spy private ServerlessV2PluginInfoProviderHelper serverlessV2PluginInfoProviderHelper;

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testValidateEnvVariables() {
    Map<String, String> environmentVariables =
        Map.of("PLUGIN_STACK_NAME", "plugin_stack_name", "PLUGIN_SAM_DIR", "sam/manifest/dir");
    Map<String, String> validatedEnvironmentVariables =
        serverlessV2PluginInfoProviderHelper.validateEnvVariables(environmentVariables);

    assertThat(validatedEnvironmentVariables).isEqualTo(environmentVariables);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testValidateEmptyEnvVariables() {
    Map<String, String> environmentVariables = Collections.emptyMap();
    Map<String, String> validatedEnvironmentVariables =
        serverlessV2PluginInfoProviderHelper.validateEnvVariables(environmentVariables);

    assertThat(validatedEnvironmentVariables).isEqualTo(environmentVariables);

    validatedEnvironmentVariables = serverlessV2PluginInfoProviderHelper.validateEnvVariables(null);

    assertThat(validatedEnvironmentVariables).isNull();
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testValidateEnvVariablesWithExceptionOneVariable() {
    Map<String, String> environmentVariables = new HashMap<>();
    environmentVariables.put("PLUGIN_STACK_NAME", null);
    environmentVariables.put("PLUGIN_SAM_DIR", "sam/manifest/dir");

    assertThatThrownBy(() -> serverlessV2PluginInfoProviderHelper.validateEnvVariables(environmentVariables))
        .hasMessage("Not found value for environment variable: PLUGIN_STACK_NAME")
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testValidateEnvVariablesWithExceptionMoreVariables() {
    Map<String, String> environmentVariables = new HashMap<>();
    environmentVariables.put("PLUGIN_STACK_NAME", null);
    environmentVariables.put("PLUGIN_SAM_DIR", null);

    assertThatThrownBy(() -> serverlessV2PluginInfoProviderHelper.validateEnvVariables(environmentVariables))
        .hasMessage("Not found value for environment variables: PLUGIN_SAM_DIR,PLUGIN_STACK_NAME")
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetEnvVarsWithSecretRef() {
    Map<String, String> environmentVariables = new HashMap<>();
    environmentVariables.put("PLUGIN_STACK_NAME", null);
    environmentVariables.put("PLUGIN_SAM_DIR", null);

    Map<String, String> map = serverlessV2PluginInfoProviderHelper.getEnvVarsWithSecretRef(environmentVariables);
    assertThat(map.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testRemoveAllEnvVarsWithSecretRef() {
    Map<String, String> environmentVariables = new HashMap<>();
    environmentVariables.put("PLUGIN_STACK_NAME", null);
    environmentVariables.put("PLUGIN_SAM_DIR", null);

    Map<String, String> map = serverlessV2PluginInfoProviderHelper.removeAllEnvVarsWithSecretRef(environmentVariables);
    assertThat(map.size()).isEqualTo(0);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetEnvironmentVariables() {
    String accountId = "accountId";
    serverlessV2PluginInfoProviderHelper.getEnvironmentVariables(
        Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build(),
        AsgBlueGreenDeployStepParameters.infoBuilder().build());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void
  testGetEnvironmentVariablesWhenStoreIsNotGitAndS3AndHarnessStoreInServerlessAwsLambdaPrepareRollbackV2StepParameters() {
    String accountId = "accountId";
    Map<String, ManifestOutcome> map = new HashMap<>();
    ManifestOutcome manifestOutcome =
        ServerlessAwsLambdaManifestOutcome.builder().store(ArtifactoryStoreConfig.builder().build()).build();
    map.put("abc", manifestOutcome);
    ManifestsOutcome manifestsOutcome = new ManifestsOutcome(map);
    doReturn(OptionalOutcome.builder().outcome(manifestsOutcome).found(true).build())
        .when(outcomeService)
        .resolveOptional(any(), any());
    doReturn(manifestOutcome).when(pluginInfoProviderUtils).getServerlessManifestOutcome(any(), any());
    serverlessV2PluginInfoProviderHelper.getEnvironmentVariables(
        Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build(),
        ServerlessAwsLambdaPrepareRollbackV2StepParameters.infoBuilder().build());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void
  testGetEnvironmentVariablesWhenStoreIsNotGitAndS3AndHarnessStoreInServerlessAwsLambdaPackageV2StepParameters() {
    String accountId = "accountId";
    Map<String, ManifestOutcome> map = new HashMap<>();
    ManifestOutcome manifestOutcome =
        ServerlessAwsLambdaManifestOutcome.builder().store(ArtifactoryStoreConfig.builder().build()).build();
    map.put("abc", manifestOutcome);
    ManifestsOutcome manifestsOutcome = new ManifestsOutcome(map);
    doReturn(OptionalOutcome.builder().outcome(manifestsOutcome).found(true).build())
        .when(outcomeService)
        .resolveOptional(any(), any());
    doReturn(manifestOutcome).when(pluginInfoProviderUtils).getServerlessManifestOutcome(any(), any());
    serverlessV2PluginInfoProviderHelper.getEnvironmentVariables(
        Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build(),
        ServerlessAwsLambdaPackageV2StepParameters.infoBuilder().build());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void
  testGetEnvironmentVariablesWhenStoreIsNotGitAndS3AndHarnessStoreInServerlessAwsLambdaDeployV2StepParameters() {
    String accountId = "accountId";
    Map<String, ManifestOutcome> map = new HashMap<>();
    ManifestOutcome manifestOutcome =
        ServerlessAwsLambdaManifestOutcome.builder().store(ArtifactoryStoreConfig.builder().build()).build();
    map.put("abc", manifestOutcome);
    ManifestsOutcome manifestsOutcome = new ManifestsOutcome(map);
    doReturn(OptionalOutcome.builder().outcome(manifestsOutcome).found(true).build())
        .when(outcomeService)
        .resolveOptional(any(), any());
    doReturn(manifestOutcome).when(pluginInfoProviderUtils).getServerlessManifestOutcome(any(), any());
    serverlessV2PluginInfoProviderHelper.getEnvironmentVariables(
        Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build(),
        ServerlessAwsLambdaDeployV2StepParameters.infoBuilder().build());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void
  testGetEnvironmentVariablesWhenStoreIsNotGitAndS3AndHarnessStoreInServerlessAwsLambdaRollbackV2StepParameters() {
    String accountId = "accountId";
    Map<String, ManifestOutcome> map = new HashMap<>();
    ManifestOutcome manifestOutcome =
        ServerlessAwsLambdaManifestOutcome.builder().store(ArtifactoryStoreConfig.builder().build()).build();
    map.put("abc", manifestOutcome);
    ManifestsOutcome manifestsOutcome = new ManifestsOutcome(map);
    doReturn(OptionalOutcome.builder().outcome(manifestsOutcome).found(true).build())
        .when(outcomeService)
        .resolveOptional(any(), any());
    doReturn(manifestOutcome).when(pluginInfoProviderUtils).getServerlessManifestOutcome(any(), any());
    serverlessV2PluginInfoProviderHelper.getEnvironmentVariables(
        Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build(),
        ServerlessAwsLambdaRollbackV2StepParameters.infoBuilder().build());
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetValuesPathFromValuesManifestOutcomeForAwsS3() {
    String identifier = "identifier";
    String path = "values.yaml";
    ValuesManifestOutcome valuesManifestOutcome =
        ValuesManifestOutcome.builder()
            .store(S3StoreConfig.builder().paths(ParameterField.createValueField(Arrays.asList(path))).build())
            .identifier(identifier)
            .build();
    String finalPath =
        serverlessV2PluginInfoProviderHelper.getValuesPathFromValuesManifestOutcome(valuesManifestOutcome);
    assertThat(finalPath).isEqualTo("/harness/" + identifier + "/" + path);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetServerlessAwsLambdaDirectoryPathFromManifestOutcomeForAwsS3() {
    String identifier = "identifier";
    String path = "values.yaml";
    ServerlessAwsLambdaManifestOutcome serverlessAwsLambdaManifestOutcome =
        ServerlessAwsLambdaManifestOutcome.builder()
            .store(S3StoreConfig.builder().paths(ParameterField.createValueField(Arrays.asList(path))).build())
            .identifier(identifier)
            .build();
    String finalPath = serverlessV2PluginInfoProviderHelper.getServerlessAwsLambdaDirectoryPathFromManifestOutcome(
        serverlessAwsLambdaManifestOutcome);
    assertThat(finalPath).isEqualTo("/harness/" + identifier);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetValuesPathFromValuesManifestOutcomeForHarnessStore() {
    String identifier = "identifier";
    String path = "values.yaml";
    ValuesManifestOutcome valuesManifestOutcome =
        ValuesManifestOutcome.builder()
            .store(HarnessStore.builder().files(ParameterField.createValueField(Arrays.asList(path))).build())
            .identifier(identifier)
            .build();
    String finalPath =
        serverlessV2PluginInfoProviderHelper.getValuesPathFromValuesManifestOutcome(valuesManifestOutcome);
    assertThat(finalPath).isEqualTo("/harness/" + identifier + "/" + path);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testGetServerlessAwsLambdaDirectoryPathFromManifestOutcomeForHarnessStore() {
    String identifier = "identifier";
    String path = "values.yaml";
    ServerlessAwsLambdaManifestOutcome serverlessAwsLambdaManifestOutcome =
        ServerlessAwsLambdaManifestOutcome.builder()
            .store(HarnessStore.builder().files(ParameterField.createValueField(Arrays.asList(path))).build())
            .identifier(identifier)
            .build();
    String finalPath = serverlessV2PluginInfoProviderHelper.getServerlessAwsLambdaDirectoryPathFromManifestOutcome(
        serverlessAwsLambdaManifestOutcome);
    assertThat(finalPath).isEqualTo("/harness/" + identifier);
  }
}
