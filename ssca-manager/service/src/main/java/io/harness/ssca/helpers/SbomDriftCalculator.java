/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.helpers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ssca.beans.drift.ComponentDrift;
import io.harness.ssca.beans.drift.ComponentDriftCalculationResult;
import io.harness.ssca.beans.drift.ComponentDriftComparator;
import io.harness.ssca.beans.drift.ComponentDriftStatus;
import io.harness.ssca.beans.drift.ComponentSummary;
import io.harness.ssca.beans.drift.LicenseDrift;
import io.harness.ssca.beans.drift.LicenseDriftCalculationResult;
import io.harness.ssca.beans.drift.LicenseDriftComparator;
import io.harness.ssca.beans.drift.LicenseDriftStatus;
import io.harness.ssca.entities.NormalizedSBOMComponentEntity.NormalizedSBOMEntityKeys;
import io.harness.ssca.services.NormalisedSbomComponentService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.SetOperators;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.SSCA)
public class SbomDriftCalculator {
  @Inject NormalisedSbomComponentService normalisedSbomComponentService;

  public List<ComponentDrift> findComponentDrifts(String baseOrchestrationId, String driftArtifactOrchestrationId) {
    List<ComponentDriftCalculationResult> componentDriftCalculationResults =
        normalisedSbomComponentService.getComponentsByAggregation(
            getComponentDriftAggregation(baseOrchestrationId, driftArtifactOrchestrationId),
            ComponentDriftCalculationResult.class);
    List<ComponentDrift> componentDrifts = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(componentDriftCalculationResults)) {
      // Our aggregation query ensures there is only one element returned in the list.
      ComponentDriftCalculationResult componentDriftCalculationResult = componentDriftCalculationResults.get(0);
      componentDrifts = mapComponentDriftResultsToComponentDrift(componentDriftCalculationResult);
    }
    return componentDrifts;
  }

  public List<LicenseDrift> findLicenseDrift(String baseOrchestrationId, String driftArtifactOrchestrationId) {
    List<LicenseDriftCalculationResult> licenseDriftCalculationResults =
        normalisedSbomComponentService.getComponentsByAggregation(
            getLicenseDriftAggregation(baseOrchestrationId, driftArtifactOrchestrationId),
            LicenseDriftCalculationResult.class);
    List<LicenseDrift> licenseDrifts = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(licenseDriftCalculationResults)) {
      // Our aggregation query ensures there is only one element returned in the list.
      LicenseDriftCalculationResult licenseDriftCalculationResult = licenseDriftCalculationResults.get(0);
      licenseDrifts = mapLicenseDriftResultsToLicenseDrift(licenseDriftCalculationResult);
    }
    return licenseDrifts;
  }

  private List<ComponentDrift> mapComponentDriftResultsToComponentDrift(
      ComponentDriftCalculationResult componentDriftCalculationResult) {
    Map<String, ComponentSummary> addedOrModifiedMap = new HashMap<>();
    Map<String, ComponentSummary> deletedOrModifiedMap = new HashMap<>();
    componentDriftCalculationResult.getAddedOrModifiedSet().forEach(e -> addedOrModifiedMap.put(e.getPackageName(), e));
    componentDriftCalculationResult.getDeletedOrModifiedSet().forEach(
        e -> deletedOrModifiedMap.put(e.getPackageName(), e));

    List<ComponentDrift> componentDrifts = new ArrayList<>();
    addedOrModifiedMap.forEach((k, v) -> {
      if (deletedOrModifiedMap.containsKey(k)) {
        if (v.equals(deletedOrModifiedMap.get(k))) {
          deletedOrModifiedMap.remove(k);
        } else {
          componentDrifts.add(ComponentDrift.builder()
                                  .status(ComponentDriftStatus.MODIFIED)
                                  .newComponent(v)
                                  .oldComponent(deletedOrModifiedMap.get(k))
                                  .build());
          deletedOrModifiedMap.remove(k);
        }
      } else {
        componentDrifts.add(
            ComponentDrift.builder().status(ComponentDriftStatus.ADDED).newComponent(v).oldComponent(null).build());
      }
    });
    deletedOrModifiedMap.forEach((k, v)
                                     -> componentDrifts.add(ComponentDrift.builder()
                                                                .status(ComponentDriftStatus.DELETED)
                                                                .newComponent(null)
                                                                .oldComponent(v)
                                                                .build()));

    // Sorting the list based on packageName. We will store sorted list in database.
    componentDrifts.sort(new ComponentDriftComparator());
    return componentDrifts;
  }

  private Aggregation getComponentDriftAggregation(String baseOrchestrationId, String orchestrationId) {
    return Aggregation.newAggregation(
        Aggregation
            .facet(getMatchOrchestrationIdAggregation(baseOrchestrationId), getComponentSummaryProjectionsAggregation())
            .as("baseComponentsSet")
            .and(getMatchOrchestrationIdAggregation(orchestrationId), getComponentSummaryProjectionsAggregation())
            .as("componentsSet"),
        Aggregation.project()
            .and(SetOperators.SetDifference.arrayAsSet("baseComponentsSet").differenceTo("componentsSet"))
            .as("deletedOrModifiedSet")
            .and(SetOperators.SetDifference.arrayAsSet("componentsSet").differenceTo("baseComponentsSet"))
            .as("addedOrModifiedSet"));
  }

  private AggregationOperation getMatchOrchestrationIdAggregation(String orchestrationId) {
    return Aggregation.match(
        Criteria.where(NormalizedSBOMEntityKeys.orchestrationId.toLowerCase()).is(orchestrationId));
  }

  private AggregationOperation getComponentSummaryProjectionsAggregation() {
    return Aggregation
        .project(NormalizedSBOMEntityKeys.packageName.toLowerCase(),
            NormalizedSBOMEntityKeys.packageVersion.toLowerCase(),
            NormalizedSBOMEntityKeys.packageSupplierName.toLowerCase(),
            NormalizedSBOMEntityKeys.packageManager.toLowerCase(),
            NormalizedSBOMEntityKeys.packageLicense.toLowerCase(), NormalizedSBOMEntityKeys.purl.toLowerCase())
        .andExclude("_id");
  }

  private Aggregation getLicenseDriftAggregation(String baseOrchestrationId, String orchestrationId) {
    return Aggregation.newAggregation(
        Aggregation
            .facet(getMatchOrchestrationIdAggregation(baseOrchestrationId), getLicenseDriftProjectionsAggregation(),
                getLicenseUnwindAggregation(), getLicenseGroupAggregation("oldLicenses"))
            .as("baseSet")
            .and(getMatchOrchestrationIdAggregation(orchestrationId), getLicenseDriftProjectionsAggregation(),
                getLicenseUnwindAggregation(), getLicenseGroupAggregation("newLicenses"))
            .as("driftSet"),
        Aggregation.unwind("$baseSet"), Aggregation.unwind("$driftSet"),
        Aggregation.project()
            .and(SetOperators.SetDifference.arrayAsSet("baseSet.oldLicenses").differenceTo("driftSet.newLicenses"))
            .as("licensesDeleted")
            .and(SetOperators.SetDifference.arrayAsSet("driftSet.newLicenses").differenceTo("baseSet.oldLicenses"))
            .as("licensesAdded"));
  }

  private AggregationOperation getLicenseDriftProjectionsAggregation() {
    return Aggregation
        .project(NormalizedSBOMEntityKeys.orchestrationId.toLowerCase(),
            NormalizedSBOMEntityKeys.packageLicense.toLowerCase())
        .andExclude("_id");
  }

  private AggregationOperation getLicenseUnwindAggregation() {
    return Aggregation.unwind("$packagelicense");
  }

  private AggregationOperation getLicenseGroupAggregation(String licenseField) {
    return Aggregation.group("$orchestrationid").addToSet("$packagelicense").as(licenseField);
  }

  private List<LicenseDrift> mapLicenseDriftResultsToLicenseDrift(LicenseDriftCalculationResult licenseDriftResults) {
    List<LicenseDrift> licenseDrifts = new ArrayList<>();
    licenseDriftResults.getLicensesAdded().forEach(
        l -> licenseDrifts.add(LicenseDrift.builder().name(l).status(LicenseDriftStatus.ADDED).build()));
    licenseDriftResults.getLicensesDeleted().forEach(
        l -> licenseDrifts.add(LicenseDrift.builder().name(l).status(LicenseDriftStatus.DELETED).build()));
    licenseDrifts.sort(new LicenseDriftComparator());
    return licenseDrifts;
  }
}
