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
import io.harness.ssca.entities.NormalizedSBOMComponentEntity;
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
    // TODO: add logic to calculate license Drift
    return new ArrayList<>();
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
        Criteria.where(NormalizedSBOMComponentEntity.NormalizedSBOMEntityKeys.orchestrationId.toLowerCase())
            .is(orchestrationId));
  }

  private AggregationOperation getComponentSummaryProjectionsAggregation() {
    return Aggregation
        .project(NormalizedSBOMComponentEntity.NormalizedSBOMEntityKeys.packageName.toLowerCase(),
            NormalizedSBOMComponentEntity.NormalizedSBOMEntityKeys.packageVersion.toLowerCase(),
            NormalizedSBOMComponentEntity.NormalizedSBOMEntityKeys.packageSupplierName.toLowerCase(),
            NormalizedSBOMComponentEntity.NormalizedSBOMEntityKeys.packageManager.toLowerCase(),
            NormalizedSBOMComponentEntity.NormalizedSBOMEntityKeys.packageLicense.toLowerCase(),
            NormalizedSBOMComponentEntity.NormalizedSBOMEntityKeys.purl.toLowerCase())
        .andExclude("_id");
  }
}
