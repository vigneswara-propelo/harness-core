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

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.containerStepGroup.ContainerStepGroupHelper;
import io.harness.connector.services.ConnectorService;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;

import com.google.inject.name.Named;
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
public class ContainerStepGroupHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Named(DEFAULT_CONNECTOR_SERVICE) @Mock private ConnectorService connectorService;

  private static String NG_SECRET_MANAGER = "ngSecretManager";
  @InjectMocks @Spy private ContainerStepGroupHelper containerStepGroupHelper;

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testValidateEnvVariables() {
    Map<String, String> environmentVariables =
        Map.of("PLUGIN_STACK_NAME", "plugin_stack_name", "PLUGIN_SAM_DIR", "sam/manifest/dir");
    Map<String, String> validatedEnvironmentVariables =
        containerStepGroupHelper.validateEnvVariables(environmentVariables);

    assertThat(validatedEnvironmentVariables).isEqualTo(environmentVariables);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testValidateEmptyEnvVariables() {
    Map<String, String> environmentVariables = Collections.emptyMap();
    Map<String, String> validatedEnvironmentVariables =
        containerStepGroupHelper.validateEnvVariables(environmentVariables);

    assertThat(validatedEnvironmentVariables).isEqualTo(environmentVariables);

    validatedEnvironmentVariables = containerStepGroupHelper.validateEnvVariables(null);

    assertThat(validatedEnvironmentVariables).isNull();
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testValidateEnvVariablesWithExceptionOneVariable() {
    Map<String, String> environmentVariables = new HashMap<>();
    environmentVariables.put("PLUGIN_STACK_NAME", null);
    environmentVariables.put("PLUGIN_SAM_DIR", "sam/manifest/dir");

    assertThatThrownBy(() -> containerStepGroupHelper.validateEnvVariables(environmentVariables))
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

    assertThatThrownBy(() -> containerStepGroupHelper.validateEnvVariables(environmentVariables))
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

    Map<String, String> map = containerStepGroupHelper.getEnvVarsWithSecretRef(environmentVariables);
    assertThat(map.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testRemoveAllEnvVarsWithSecretRef() {
    Map<String, String> environmentVariables = new HashMap<>();
    environmentVariables.put("PLUGIN_STACK_NAME", null);
    environmentVariables.put("PLUGIN_SAM_DIR", null);

    Map<String, String> map = containerStepGroupHelper.removeAllEnvVarsWithSecretRef(environmentVariables);
    assertThat(map.size()).isEqualTo(0);
  }
}
