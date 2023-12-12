/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.k8s.trafficrouting;

import static io.harness.rule.OwnerRule.BUHA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.trafficrouting.HeaderConfig;
import io.harness.delegate.task.k8s.trafficrouting.IstioProviderConfig;
import io.harness.delegate.task.k8s.trafficrouting.K8sTrafficRoutingConfig;
import io.harness.delegate.task.k8s.trafficrouting.MatchType;
import io.harness.delegate.task.k8s.trafficrouting.RouteType;
import io.harness.delegate.task.k8s.trafficrouting.RuleType;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRoute;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRouteRule;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRoutingDestination;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.rule.Owner;

import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class IstioTrafficRoutingMapperTest extends CategoryTest {
  private final String namespace = "namespace";
  private final String releaseName = "release-name";
  private final String apiVersion = "networking.istio.io/v1alpha3";
  private final KubernetesResource stableService =
      KubernetesResource.builder().resourceId(KubernetesResourceId.builder().name("stableService").build()).build();
  private final KubernetesResource stageService =
      KubernetesResource.builder().resourceId(KubernetesResourceId.builder().name("stageService").build()).build();

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithOnlyDestinations() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig =
        K8sTrafficRoutingConfig.builder()
            .destinations(List.of(TrafficRoutingDestination.builder().host("stable").weight(80).build(),
                TrafficRoutingDestination.builder().host("stage").weight(20).build()))
            .routes(List.of(TrafficRoute.builder().routeType(RouteType.HTTP).build()))
            .providerConfig(IstioProviderConfig.builder().build())
            .build();
    String path = "/k8s/trafficrouting/virtualService1.yaml";

    List<String> trafficRoutingManifests = IstioTrafficRoutingMapper.getTrafficRoutingManifests(
        istioProviderConfig, namespace, releaseName, stableService, stageService, apiVersion);

    assertThat(trafficRoutingManifests.size()).isEqualTo(1);
    assertEqualYaml(trafficRoutingManifests.get(0), path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithHttpRouteUriRule() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig = getProviderConfig(RuleType.URI, MatchType.EXACT, "dummyValue");
    String path = "/k8s/trafficrouting/virtualServiceUriRule.yaml";

    List<String> trafficRoutingManifests = IstioTrafficRoutingMapper.getTrafficRoutingManifests(
        istioProviderConfig, namespace, releaseName, stableService, stageService, apiVersion);

    assertThat(trafficRoutingManifests.size()).isEqualTo(1);
    assertEqualYaml(trafficRoutingManifests.get(0), path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithHttpRouteSchemeRule() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig = getProviderConfig(RuleType.SCHEME, MatchType.EXACT, "dummyValue");
    String path = "/k8s/trafficrouting/virtualServiceSchemeRule.yaml";

    List<String> trafficRoutingManifests = IstioTrafficRoutingMapper.getTrafficRoutingManifests(
        istioProviderConfig, namespace, releaseName, stableService, stageService, apiVersion);

    assertThat(trafficRoutingManifests.size()).isEqualTo(1);
    assertEqualYaml(trafficRoutingManifests.get(0), path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithHttpRouteMethodRule() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig = getProviderConfig(RuleType.METHOD, MatchType.EXACT, "GET");
    String path = "/k8s/trafficrouting/virtualServiceMethodRule.yaml";

    List<String> trafficRoutingManifests = IstioTrafficRoutingMapper.getTrafficRoutingManifests(
        istioProviderConfig, namespace, releaseName, stableService, stageService, apiVersion);

    assertThat(trafficRoutingManifests.size()).isEqualTo(1);
    assertEqualYaml(trafficRoutingManifests.get(0), path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithHttpRouteAuthorityRule() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig = getProviderConfig(RuleType.AUTHORITY, MatchType.REGEX, "dummyValue");
    String path = "/k8s/trafficrouting/virtualServiceAuthorityRule.yaml";

    List<String> trafficRoutingManifests = IstioTrafficRoutingMapper.getTrafficRoutingManifests(
        istioProviderConfig, namespace, releaseName, stableService, stageService, apiVersion);

    assertThat(trafficRoutingManifests.size()).isEqualTo(1);
    assertEqualYaml(trafficRoutingManifests.get(0), path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithHttpRouteHeaderRule() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig = getProviderConfig(RuleType.HEADER, MatchType.REGEX, null);
    String path = "/k8s/trafficrouting/virtualServiceHeaderRule.yaml";

    List<String> trafficRoutingManifests = IstioTrafficRoutingMapper.getTrafficRoutingManifests(
        istioProviderConfig, namespace, releaseName, stableService, stageService, apiVersion);

    assertThat(trafficRoutingManifests.size()).isEqualTo(1);
    assertEqualYaml(trafficRoutingManifests.get(0), path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithHttpRoutePortRule() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig = getProviderConfig(RuleType.PORT, MatchType.EXACT, "8080");
    String path = "/k8s/trafficrouting/virtualServicePortRule.yaml";

    List<String> trafficRoutingManifests = IstioTrafficRoutingMapper.getTrafficRoutingManifests(
        istioProviderConfig, namespace, releaseName, stableService, stageService, apiVersion);

    assertThat(trafficRoutingManifests.size()).isEqualTo(1);
    assertEqualYaml(trafficRoutingManifests.get(0), path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWhenDestinationWeightIsNotProvided() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig =
        K8sTrafficRoutingConfig.builder()
            .destinations(List.of(TrafficRoutingDestination.builder().host("stable").weight(0).build(),
                TrafficRoutingDestination.builder().host("stage").weight(0).build()))
            .routes(List.of(TrafficRoute.builder().routeType(RouteType.HTTP).build()))
            .providerConfig(IstioProviderConfig.builder().build())
            .build();
    String path = "/k8s/trafficrouting/virtualService2.yaml";

    List<String> trafficRoutingManifests = IstioTrafficRoutingMapper.getTrafficRoutingManifests(
        istioProviderConfig, namespace, releaseName, stableService, stageService, apiVersion);

    assertThat(trafficRoutingManifests.size()).isEqualTo(1);
    assertEqualYaml(trafficRoutingManifests.get(0), path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsNormalizeWeight() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig =
        K8sTrafficRoutingConfig.builder()
            .destinations(List.of(TrafficRoutingDestination.builder().host("stable").weight(7).build(),
                TrafficRoutingDestination.builder().host("stage").weight(3).build()))
            .routes(List.of(TrafficRoute.builder().routeType(RouteType.HTTP).build()))
            .providerConfig(IstioProviderConfig.builder().build())
            .build();
    String path = "/k8s/trafficrouting/virtualService3.yaml";

    List<String> trafficRoutingManifests = IstioTrafficRoutingMapper.getTrafficRoutingManifests(
        istioProviderConfig, namespace, releaseName, stableService, stageService, apiVersion);

    assertThat(trafficRoutingManifests.size()).isEqualTo(1);
    assertEqualYaml(trafficRoutingManifests.get(0), path);
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithoutStableOrStage() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig =
        K8sTrafficRoutingConfig.builder()
            .destinations(List.of(TrafficRoutingDestination.builder().host("firstService").weight(6).build(),
                TrafficRoutingDestination.builder().host("secondService").weight(2).build(),
                TrafficRoutingDestination.builder().host("thirdService").weight(2).build()))
            .routes(List.of(TrafficRoute.builder().routeType(RouteType.HTTP).build()))
            .providerConfig(IstioProviderConfig.builder().build())
            .build();

    IstioTrafficRoutingMapper.getTrafficRoutingManifests(
        istioProviderConfig, namespace, releaseName, null, null, apiVersion);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsWithoutStableOrStageWithHost() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig =
        K8sTrafficRoutingConfig.builder()
            .providerConfig(IstioProviderConfig.builder().hosts(List.of("root-host")).build())
            .destinations(List.of(TrafficRoutingDestination.builder().host("firstService").weight(6).build(),
                TrafficRoutingDestination.builder().host("secondService").weight(2).build(),
                TrafficRoutingDestination.builder().host("thirdService").weight(2).build()))
            .routes(List.of(TrafficRoute.builder().routeType(RouteType.HTTP).build()))
            .build();

    String path = "/k8s/trafficrouting/virtualService4.yaml";

    List<String> trafficRoutingManifests = IstioTrafficRoutingMapper.getTrafficRoutingManifests(
        istioProviderConfig, namespace, releaseName, null, null, apiVersion);

    assertThat(trafficRoutingManifests.size()).isEqualTo(1);
    assertEqualYaml(trafficRoutingManifests.get(0), path);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingManifestsMultipleRoutes() throws IOException {
    K8sTrafficRoutingConfig istioProviderConfig =
        K8sTrafficRoutingConfig.builder()
            .providerConfig(IstioProviderConfig.builder().hosts(List.of("root-host")).build())
            .destinations(List.of(TrafficRoutingDestination.builder().host("stable").weight(0).build(),
                TrafficRoutingDestination.builder().host("stage").weight(0).build()))
            .routes(List.of(TrafficRoute.builder()
                                .routeType(RouteType.HTTP)
                                .rules(List.of(TrafficRouteRule.builder()
                                                   .ruleType(RuleType.URI)
                                                   .matchType(MatchType.EXACT)
                                                   .value("value1")
                                                   .name("rule1")
                                                   .build()))
                                .build(),
                TrafficRoute.builder()
                    .routeType(RouteType.HTTP)
                    .rules(List.of(TrafficRouteRule.builder()
                                       .ruleType(RuleType.URI)
                                       .matchType(MatchType.EXACT)
                                       .value("value2")
                                       .name("rule2")
                                       .build()))
                    .build()))
            .build();

    String path = "/k8s/trafficrouting/virtualService5.yaml";

    List<String> trafficRoutingManifests = IstioTrafficRoutingMapper.getTrafficRoutingManifests(
        istioProviderConfig, namespace, releaseName, stableService, stageService, apiVersion);

    assertThat(trafficRoutingManifests.size()).isEqualTo(1);
    assertEqualYaml(trafficRoutingManifests.get(0), path);
  }

  private void assertEqualYaml(String yaml, String path) throws IOException {
    URL url = this.getClass().getResource(path);
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    assertThat(yaml).isEqualTo(fileContents);
  }

  private K8sTrafficRoutingConfig getProviderConfig(RuleType ruleType, MatchType matchType, String value) {
    return K8sTrafficRoutingConfig.builder()
        .destinations(List.of(TrafficRoutingDestination.builder().host("stable").weight(80).build(),
            TrafficRoutingDestination.builder().host("stage").weight(20).build()))
        .providerConfig(IstioProviderConfig.builder().build())
        .routes(List.of(
            TrafficRoute.builder()
                .routeType(RouteType.HTTP)
                .rules(List.of(TrafficRouteRule.builder()
                                   .ruleType(ruleType)
                                   .value(value)
                                   .values(List.of("8080", "8081", "8082"))
                                   .headerConfigs(List.of(
                                       HeaderConfig.builder().matchType(matchType).key("key").value("value").build()))
                                   .matchType(matchType)
                                   .build()))
                .build()))
        .build();
  }
}
