/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.environment;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGTestBase;
import io.harness.cdng.environment.steps.EnvironmentStepParameters;
import io.harness.cdng.environment.yaml.EnvironmentPlanCreatorConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.yaml.core.variables.NGServiceOverrides;

import java.util.Arrays;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDC)
public class EnvPlanCreatorConfigHelperTest extends CDNGTestBase {
  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetFieldClass() {
    EnvironmentPlanCreatorConfig environmentPlanCreatorConfig =
        EnvironmentPlanCreatorConfig.builder()
            .environmentRef(ParameterField.createValueField("ref"))
            .name("name")
            .identifier("identifier")
            .description("description")
            .serviceOverrides(Arrays.asList(NGServiceOverrides.builder().serviceRef("ser").build()))
            .build();
    EnvironmentStepParameters environmentStepParameters =
        EnvironmentPlanCreatorConfigHelper.toEnvironmentStepParameters(environmentPlanCreatorConfig);
    assertThat(environmentStepParameters.getEnvironmentRef().getValue()).isEqualTo("ref");
    assertThat(environmentStepParameters.getIdentifier()).isEqualTo("identifier");
    assertThat(environmentStepParameters.getName()).isEqualTo("name");
    assertThat(environmentStepParameters.getDescription()).isEqualTo("description");
    assertThat(environmentStepParameters.getServiceOverrides().size()).isEqualTo(1);
  }
}