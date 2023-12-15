/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.mapper;

import static io.harness.rule.OwnerRule.INDER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.SSCAManagerTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.spec.server.ssca.v1.model.OrchestrationStepDriftRequestBody;
import io.harness.ssca.beans.drift.ComponentDrift;
import io.harness.ssca.beans.drift.ComponentDriftStatus;
import io.harness.ssca.beans.drift.ComponentSummary;
import io.harness.ssca.beans.drift.DriftBase;
import io.harness.ssca.beans.drift.LicenseDrift;
import io.harness.ssca.beans.drift.LicenseDriftStatus;

import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.SSCA)
public class SbomDriftMapperTest extends SSCAManagerTestBase {
  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testMapStatusToComponentDriftStatus() {
    assertThat(SbomDriftMapper.mapStatusToComponentDriftStatus("added")).isEqualTo(ComponentDriftStatus.ADDED);
    assertThat(SbomDriftMapper.mapStatusToComponentDriftStatus("modified")).isEqualTo(ComponentDriftStatus.MODIFIED);
    assertThat(SbomDriftMapper.mapStatusToComponentDriftStatus("deleted")).isEqualTo(ComponentDriftStatus.DELETED);
    assertThat(SbomDriftMapper.mapStatusToComponentDriftStatus("all")).isEqualTo(null);
    assertThatThrownBy(() -> SbomDriftMapper.mapStatusToComponentDriftStatus("abc"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("status could only be one of all / added / modified / deleted");
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testMapStatusToLicenseDriftStatus() {
    assertThat(SbomDriftMapper.mapStatusToLicenseDriftStatus("added")).isEqualTo(LicenseDriftStatus.ADDED);
    assertThat(SbomDriftMapper.mapStatusToLicenseDriftStatus("deleted")).isEqualTo(LicenseDriftStatus.DELETED);
    assertThat(SbomDriftMapper.mapStatusToLicenseDriftStatus("all")).isEqualTo(null);
    assertThatThrownBy(() -> SbomDriftMapper.mapStatusToLicenseDriftStatus("abc"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("status could only be one of all / added / deleted");
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testGetDriftBase() {
    assertThat(SbomDriftMapper.getDriftBase(new OrchestrationStepDriftRequestBody().base("baseline")))
        .isEqualTo(DriftBase.BASELINE);
    assertThat(SbomDriftMapper.getDriftBase(new OrchestrationStepDriftRequestBody().base("last_generated_sbom")))
        .isEqualTo(DriftBase.LAST_GENERATED_SBOM);
    assertThatThrownBy(() -> SbomDriftMapper.getDriftBase(new OrchestrationStepDriftRequestBody().base("abc")))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Base could only be baseline / last_generated_sbom");
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testToComponentDriftResponseList() {
    assertThat(
        SbomDriftMapper.toComponentDriftResponseList(List.of(
            ComponentDrift.builder().status(ComponentDriftStatus.ADDED).newComponent(getComponentSummary()).build())))
        .isEqualTo(List.of(new io.harness.spec.server.ssca.v1.model.ComponentDrift().status("added").newComponent(
            new io.harness.spec.server.ssca.v1.model.ComponentSummary()
                .purl("purl")
                .packageName("name")
                .packageLicense("l1, l2")
                .packageVersion("version")
                .packageSupplier("supplier")
                .packageManager("packageManager"))));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testToLicenseDriftResponseList() {
    assertThat(SbomDriftMapper.toLicenseDriftResponseList(List.of(LicenseDrift.builder()
                                                                      .status(LicenseDriftStatus.DELETED)
                                                                      .name("l1")
                                                                      .components(List.of(getComponentSummary()))
                                                                      .build())))
        .isEqualTo(
            List.of(new io.harness.spec.server.ssca.v1.model.LicenseDrift().status("deleted").license("l1").components(
                List.of(new io.harness.spec.server.ssca.v1.model.ComponentSummary()
                            .purl("purl")
                            .packageName("name")
                            .packageLicense("l1, l2")
                            .packageVersion("version")
                            .packageSupplier("supplier")
                            .packageManager("packageManager")))));
  }

  private ComponentSummary getComponentSummary() {
    return ComponentSummary.builder()
        .packageName("name")
        .packageLicense(List.of("l1", "l2"))
        .packageVersion("version")
        .purl("purl")
        .packageOriginatorName("supplier")
        .packageManager("packageManager")
        .build();
  }
}
