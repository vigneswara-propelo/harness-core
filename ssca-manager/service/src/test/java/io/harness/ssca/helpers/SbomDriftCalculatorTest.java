/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.helpers;

import static io.harness.rule.OwnerRule.INDER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.SSCAManagerTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.ssca.beans.drift.ComponentDrift;
import io.harness.ssca.beans.drift.ComponentDriftCalculationResult;
import io.harness.ssca.beans.drift.ComponentDriftStatus;
import io.harness.ssca.beans.drift.ComponentSummary;
import io.harness.ssca.beans.drift.LicenseDrift;
import io.harness.ssca.beans.drift.LicenseDriftCalculationResult;
import io.harness.ssca.beans.drift.LicenseDriftStatus;
import io.harness.ssca.services.NormalisedSbomComponentService;

import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.mongodb.core.aggregation.Aggregation;

@OwnedBy(HarnessTeam.SSCA)
public class SbomDriftCalculatorTest extends SSCAManagerTestBase {
  @InjectMocks SbomDriftCalculator sbomDriftCalculator;
  @Mock NormalisedSbomComponentService normalisedSbomComponentService;

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testFindComponentDrifts() {
    when(normalisedSbomComponentService.getComponentsByAggregation(any(), any()))
        .thenReturn(List.of(
            ComponentDriftCalculationResult.builder()
                .addedOrModifiedSet(Set.of(
                    ComponentSummary.builder().packageName("name").purl("purl").packageVersion("version").build(),
                    ComponentSummary.builder().packageName("name1").purl("purl").packageVersion("version1").build()))
                .deletedOrModifiedSet(Set.of(
                    ComponentSummary.builder().packageName("name1").purl("purl").packageVersion("version").build(),
                    ComponentSummary.builder().packageName("name2").purl("purl").packageVersion("version2").build()))
                .build()));

    ArgumentCaptor<Aggregation> argumentCaptor = ArgumentCaptor.forClass(Aggregation.class);

    List<ComponentDrift> componentDrifts = sbomDriftCalculator.findComponentDrifts("base", "tag");
    verify(normalisedSbomComponentService)
        .getComponentsByAggregation(argumentCaptor.capture(), eq(ComponentDriftCalculationResult.class));
    Aggregation aggregation = argumentCaptor.getValue();
    assertThat(aggregation.toString())
        .isEqualTo(
            "{ \"aggregate\" : \"__collection__\", \"pipeline\" : [{ \"$facet\" : { \"baseComponentsSet\" : [{ \"$match\" : { \"orchestrationid\" : \"base\"}}, { \"$project\" : { \"packagename\" : 1, \"packageversion\" : 1, \"packagesuppliername\" : 1, \"packagemanager\" : 1, \"packagelicense\" : 1, \"purl\" : 1, \"_id\" : 0}}], \"componentsSet\" : [{ \"$match\" : { \"orchestrationid\" : \"tag\"}}, { \"$project\" : { \"packagename\" : 1, \"packageversion\" : 1, \"packagesuppliername\" : 1, \"packagemanager\" : 1, \"packagelicense\" : 1, \"purl\" : 1, \"_id\" : 0}}]}}, { \"$project\" : { \"deletedOrModifiedSet\" : { \"$setDifference\" : [\"$baseComponentsSet\", \"$componentsSet\"]}, \"addedOrModifiedSet\" : { \"$setDifference\" : [\"$componentsSet\", \"$baseComponentsSet\"]}}}]}");
    assertThat(componentDrifts).hasSize(3);
    assertThat(componentDrifts.get(0))
        .isEqualTo(
            ComponentDrift.builder()
                .status(ComponentDriftStatus.ADDED)
                .oldComponent(null)
                .newComponent(
                    ComponentSummary.builder().packageName("name").purl("purl").packageVersion("version").build())
                .build());
    assertThat(componentDrifts.get(1))
        .isEqualTo(
            ComponentDrift.builder()
                .status(ComponentDriftStatus.MODIFIED)
                .newComponent(
                    ComponentSummary.builder().packageName("name1").purl("purl").packageVersion("version1").build())
                .oldComponent(
                    ComponentSummary.builder().packageName("name1").purl("purl").packageVersion("version").build())
                .build());
    assertThat(componentDrifts.get(2))
        .isEqualTo(
            ComponentDrift.builder()
                .status(ComponentDriftStatus.DELETED)
                .newComponent(null)
                .oldComponent(
                    ComponentSummary.builder().packageName("name2").purl("purl").packageVersion("version2").build())
                .build());
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testFindLicenseDrifts() {
    when(normalisedSbomComponentService.getComponentsByAggregation(any(), any()))
        .thenReturn(List.of(LicenseDriftCalculationResult.builder()
                                .licensesAdded(Set.of("a1", "f1", "l1"))
                                .licensesDeleted(Set.of("b1", "g1"))
                                .build()));

    ArgumentCaptor<Aggregation> argumentCaptor = ArgumentCaptor.forClass(Aggregation.class);

    List<LicenseDrift> licenseDrifts = sbomDriftCalculator.findLicenseDrift("base", "tag");
    verify(normalisedSbomComponentService)
        .getComponentsByAggregation(argumentCaptor.capture(), eq(LicenseDriftCalculationResult.class));
    Aggregation aggregation = argumentCaptor.getValue();
    assertThat(aggregation.toString())
        .isEqualTo(
            "{ \"aggregate\" : \"__collection__\", \"pipeline\" : [{ \"$facet\" : { \"baseSet\" : [{ \"$match\" : { \"orchestrationid\" : \"base\"}}, { \"$project\" : { \"orchestrationid\" : 1, \"packagelicense\" : 1, \"_id\" : 0}}, { \"$unwind\" : \"$packagelicense\"}, { \"$group\" : { \"_id\" : \"$orchestrationid\", \"oldLicenses\" : { \"$addToSet\" : \"$packagelicense\"}}}], \"driftSet\" : [{ \"$match\" : { \"orchestrationid\" : \"tag\"}}, { \"$project\" : { \"orchestrationid\" : 1, \"packagelicense\" : 1, \"_id\" : 0}}, { \"$unwind\" : \"$packagelicense\"}, { \"$group\" : { \"_id\" : \"$orchestrationid\", \"newLicenses\" : { \"$addToSet\" : \"$packagelicense\"}}}]}}, { \"$unwind\" : \"$baseSet\"}, { \"$unwind\" : \"$driftSet\"}, { \"$project\" : { \"licensesAdded\" : { \"$setDifference\" : [\"$baseSet.oldLicenses\", \"$driftSet.newLicenses\"]}, \"licensesDeleted\" : { \"$setDifference\" : [\"$driftSet.newLicenses\", \"$baseSet.oldLicenses\"]}}}]}");
    assertThat(licenseDrifts).hasSize(5);
    assertThat(licenseDrifts.get(0))
        .isEqualTo(LicenseDrift.builder().status(LicenseDriftStatus.ADDED).name("a1").build());
    assertThat(licenseDrifts.get(1))
        .isEqualTo(LicenseDrift.builder().status(LicenseDriftStatus.DELETED).name("b1").build());
    assertThat(licenseDrifts.get(2))
        .isEqualTo(LicenseDrift.builder().status(LicenseDriftStatus.ADDED).name("f1").build());
    assertThat(licenseDrifts.get(3))
        .isEqualTo(LicenseDrift.builder().status(LicenseDriftStatus.DELETED).name("g1").build());
    assertThat(licenseDrifts.get(4))
        .isEqualTo(LicenseDrift.builder().status(LicenseDriftStatus.ADDED).name("l1").build());
  }
}
