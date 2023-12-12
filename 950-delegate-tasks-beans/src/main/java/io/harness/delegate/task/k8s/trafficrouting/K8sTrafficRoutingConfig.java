/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.k8s.trafficrouting;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_K8S})
@SuperBuilder
@Data
@Builder
public class K8sTrafficRoutingConfig {
  List<TrafficRoute> routes;
  List<TrafficRoutingDestination> destinations;
  ProviderConfig providerConfig;

  public List<TrafficRoutingDestination> getNormalizedDestinations() {
    int sum = destinations.stream()
                  .filter(destination -> destination.getWeight() != null)
                  .mapToInt(TrafficRoutingDestination::getWeight)
                  .sum();
    return destinations.stream()
        .map(dest
            -> TrafficRoutingDestination.builder()
                   .host(dest.getHost())
                   .weight(normalize(sum, dest.getWeight()))
                   .build())
        .collect(Collectors.toList());
  }

  private int normalize(int sum, Integer weight) {
    if (sum == 0) {
      return 100 / destinations.size();
    }
    return weight == null ? 0 : weight * 100 / sum;
  }
}
