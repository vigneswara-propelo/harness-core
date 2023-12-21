/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.k8s.trafficrouting;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.task.k8s.trafficrouting.RouteType.HTTP;

import static java.lang.String.format;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.delegate.task.k8s.trafficrouting.HeaderConfig;
import io.harness.delegate.task.k8s.trafficrouting.IstioProviderConfig;
import io.harness.delegate.task.k8s.trafficrouting.K8sTrafficRoutingConfig;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRoute;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRouteRule;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRoutingDestination;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.k8s.exception.KubernetesExceptionExplanation;
import io.harness.k8s.exception.KubernetesExceptionHints;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.istio.Destination;
import io.harness.k8s.model.istio.HttpRouteDestination;
import io.harness.k8s.model.istio.Match;
import io.harness.k8s.model.istio.Metadata;
import io.harness.k8s.model.istio.VirtualService;
import io.harness.k8s.model.istio.VirtualServiceDetails;
import io.harness.k8s.model.istio.VirtualServiceSpec;

import io.kubernetes.client.util.Yaml;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
public class IstioTrafficRoutingResourceCreator extends TrafficRoutingResourceCreator {
  private static final String VS_SUFFIX = "-virtual-service";
  // toDo this needs to be revisited, should not be hardcoded
  private static final String TRAFFIC_ROUTING_STEP_VIRTUAL_SERVICE = "harness-traffic-routing-virtual-service";
  private static final String NETWORKING = "networking";
  private static final Map<String, List<String>> SUPPORTED_API_MAP =
      Map.of(NETWORKING, List.of("networking.istio.io/v1alpha3", "networking.istio.io/v1beta1"));

  public IstioTrafficRoutingResourceCreator(K8sTrafficRoutingConfig k8sTrafficRoutingConfig) {
    super(k8sTrafficRoutingConfig);
  }

  @Override
  protected List<String> getManifests(
      String namespace, String releaseName, String stableName, String stageName, Map<String, String> apiVersions) {
    String virtualServiceName =
        getTrafficRoutingResourceName(stableName, VS_SUFFIX, TRAFFIC_ROUTING_STEP_VIRTUAL_SERVICE);
    VirtualService vs = VirtualService.builder()
                            .metadata(Metadata.builder()
                                          .name(virtualServiceName)
                                          .namespace(namespace)
                                          .labels(Map.of(HarnessLabels.releaseName, releaseName))
                                          .build())
                            .apiVersion(apiVersions.get(NETWORKING))
                            .spec(getVirtualServiceSpec(stableName, stageName))
                            .build();
    return List.of(Yaml.dump(vs));
  }

  @Override
  protected Map<String, List<String>> getProviderVersionMap() {
    return SUPPORTED_API_MAP;
  }

  private VirtualServiceSpec getVirtualServiceSpec(String stableName, String stageName) {
    IstioProviderConfig providerConfig = (IstioProviderConfig) k8sTrafficRoutingConfig.getProviderConfig();
    List<String> hosts = providerConfig.getHosts();
    if (isEmpty(hosts)) {
      if (isEmpty(stableName)) {
        throw new InvalidArgumentsException("Hosts should be specified in the Istio Traffic Routing Config");
      }
      hosts = List.of(stableName);
    }

    return VirtualServiceSpec.builder()
        .gateways(providerConfig.getGateways())
        .hosts(hosts)
        .http(getHttpRouteSpec(k8sTrafficRoutingConfig.getRoutes(), k8sTrafficRoutingConfig.getNormalizedDestinations(),
            stableName, stageName))
        .build();
  }

  private List<VirtualServiceDetails> getHttpRouteSpec(
      List<TrafficRoute> routes, List<TrafficRoutingDestination> destinations, String stableName, String stageName) {
    if (routes == null) {
      throw NestedExceptionUtils.hintWithExplanationException(
          format(KubernetesExceptionHints.TRAFFIC_ROUTING_MISSING_FIELD, "routes", "ISTIO"),
          format(KubernetesExceptionExplanation.TRAFFIC_ROUTING_MISSING_FIELD, "routes"),
          new InvalidArgumentsException("Traffic Routes are missing in the Traffic Routing configuration"));
    }
    return routes.stream()
        .filter(route -> route.getRouteType() == HTTP)
        .map(route
            -> VirtualServiceDetails.builder()
                   .match(getIstioMatch(route.getRules()))
                   .route(getRouteDestinations(destinations, stableName, stageName))
                   .build())
        .collect(Collectors.toList());
  }

  private List<Match> getIstioMatch(List<TrafficRouteRule> rules) {
    if (rules == null) {
      return null;
    }
    return rules.stream()
        .map(rule
            -> Match.createMatch(rule.getRuleType().name(), rule.getName(), rule.getValue(), rule.getMatchType().name(),
                mapHeaderConfigs(rule.getHeaderConfigs())))
        .collect(Collectors.toList());
  }

  private Map<String, Pair<String, String>> mapHeaderConfigs(List<HeaderConfig> headerConfigs) {
    return headerConfigs != null ? headerConfigs.stream().collect(Collectors.toMap(HeaderConfig::getKey,
               headerConfig -> Pair.of(headerConfig.getValue(), headerConfig.getMatchType().name())))
                                 : Collections.emptyMap();
  }

  private List<HttpRouteDestination> getRouteDestinations(
      List<TrafficRoutingDestination> destinations, String stableName, String stageName) {
    return destinations.stream()
        .map(destination
            -> HttpRouteDestination.builder()
                   .weight(destination.getWeight())
                   .destination(Destination.builder()
                                    .host(updatePlaceHoldersIfExist(destination.getHost(), stableName, stageName))
                                    .build())
                   .build())
        .collect(Collectors.toList());
  }
}
