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
import io.harness.delegate.task.k8s.trafficrouting.IstioProviderConfig;
import io.harness.delegate.task.k8s.trafficrouting.K8sTrafficRoutingConfig;
import io.harness.delegate.task.k8s.trafficrouting.SMIProviderConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class K8sTrafficRoutingHelperTest extends CategoryTest {
  @InjectMocks K8sTrafficRoutingHelper k8sTrafficRoutingHelper;
  @Mock LogCallback logCallback;
  private final String namespace = "namespace";
  private final String releaseName = "release-name";
  private final String apiVersion = "networking.istio.io/v1alpha3";
  private final Set<String> availableApiVersions = Set.of("first", "second", apiVersion);
  private final KubernetesResource stableService =
      KubernetesResource.builder().resourceId(KubernetesResourceId.builder().name("stableService").build()).build();
  private final KubernetesResource stageService =
      KubernetesResource.builder().resourceId(KubernetesResourceId.builder().name("stageService").build()).build();
  private final K8sTrafficRoutingConfig k8sIstioTrafficRoutingConfig =
      K8sTrafficRoutingConfig.builder().providerConfig(IstioProviderConfig.builder().build()).build();

  private final K8sTrafficRoutingConfig k8sSMITrafficRoutingConfig =
      K8sTrafficRoutingConfig.builder().providerConfig(SMIProviderConfig.builder().build()).build();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetIstioPrividerConfig() throws IOException {
    URL url = this.getClass().getResource("/k8s/trafficrouting/virtualService1.yaml");
    String virtualServiceYaml = Resources.toString(url, Charsets.UTF_8);
    try (MockedStatic<IstioTrafficRoutingMapper> utilities = Mockito.mockStatic(IstioTrafficRoutingMapper.class)) {
      utilities
          .when(()
                    -> IstioTrafficRoutingMapper.getTrafficRoutingManifests(
                        k8sIstioTrafficRoutingConfig, namespace, releaseName, stableService, stageService, apiVersion))
          .thenReturn(List.of(virtualServiceYaml));

      List<KubernetesResource> trafficRoutingResources =
          k8sTrafficRoutingHelper.getTrafficRoutingResources(k8sIstioTrafficRoutingConfig, namespace, releaseName,
              stableService, stageService, availableApiVersions, logCallback);

      assertThat(trafficRoutingResources.size()).isEqualTo(1);
      assertThat(trafficRoutingResources.get(0).getResourceId().getName()).isEqualTo("stableService-virtual-service");
      assertThat(trafficRoutingResources.get(0).getResourceId().getNamespace()).isEqualTo("namespace");
      assertThat(trafficRoutingResources.get(0).getResourceId().getKind()).isEqualTo("VirtualService");
    }
  }

  @Test(expected = UnsupportedOperationException.class)
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetSMIPrividerConfig() {
    k8sTrafficRoutingHelper.getTrafficRoutingResources(k8sSMITrafficRoutingConfig, namespace, releaseName,
        stableService, stageService, availableApiVersions, logCallback);
  }
}
