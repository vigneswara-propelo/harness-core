/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.k8s.trafficrouting;

import static io.harness.delegate.k8s.trafficrouting.IstioTrafficRoutingMapper.ApiVersion.V1_ALPHA3;
import static io.harness.delegate.k8s.trafficrouting.IstioTrafficRoutingMapper.ApiVersion.inverseOrderVersions;
import static io.harness.logging.LogLevel.INFO;

import static java.lang.String.format;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.delegate.task.k8s.trafficrouting.K8sTrafficRoutingConfig;
import io.harness.delegate.task.k8s.trafficrouting.ProviderType;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.KubernetesResource;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import com.google.inject.Singleton;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
public class K8sTrafficRoutingHelper {
  public List<KubernetesResource> getTrafficRoutingResources(K8sTrafficRoutingConfig k8sTrafficRoutingConfig,
      String namespace, String releaseName, KubernetesResource primaryService, KubernetesResource secondaryService,
      Set<String> availableApiVersions, LogCallback logCallback) {
    List<String> trafficRoutingManifests;
    ProviderType providerType = k8sTrafficRoutingConfig.getProviderConfig().getProviderType();
    switch (providerType) {
      case SMI: {
        // toDo add logic for SMI provider
        throw new UnsupportedOperationException("Unsupported provider");
      }
      case ISTIO: {
        String apiVersion = TrafficRoutingCommon.getApiVersion(
            availableApiVersions, inverseOrderVersions(), V1_ALPHA3.version, providerType.name(), logCallback);
        trafficRoutingManifests = IstioTrafficRoutingMapper.getTrafficRoutingManifests(
            k8sTrafficRoutingConfig, namespace, releaseName, primaryService, secondaryService, apiVersion);
        break;
      }
      default:
        throw new UnsupportedOperationException("Unsupported provider type");
    }

    logCallback.saveExecutionLog(
        format("Traffic Routing resources created: %n%s", String.join("%n---", trafficRoutingManifests)), INFO,
        CommandExecutionStatus.SUCCESS);
    return trafficRoutingManifests.stream()
        .map(ManifestHelper::getKubernetesResourcesFromSpec)
        .flatMap(List::stream)
        .toList();
  }
}
