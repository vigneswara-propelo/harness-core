/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.k8s.trafficrouting;

import static io.harness.rule.OwnerRule.BUHA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class K8sTrafficRoutingConfigTest extends CategoryTest {
  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetNormalizedDestinations() {
    List<TrafficRoutingDestination> destinations =
        List.of(TrafficRoutingDestination.builder().host("host1").weight(20).build(),
            TrafficRoutingDestination.builder().host("host2").weight(30).build(),
            TrafficRoutingDestination.builder().host("host3").weight(50).build());
    K8sTrafficRoutingConfig config = K8sTrafficRoutingConfig.builder().destinations(destinations).build();

    List<TrafficRoutingDestination> normalizedDestinations = config.getNormalizedDestinations();

    assertThat(normalizedDestinations.get(0).getHost()).isEqualTo("host1");
    assertThat(normalizedDestinations.get(0).getWeight()).isEqualTo(20);
    assertThat(normalizedDestinations.get(1).getHost()).isEqualTo("host2");
    assertThat(normalizedDestinations.get(1).getWeight()).isEqualTo(30);
    assertThat(normalizedDestinations.get(2).getHost()).isEqualTo("host3");
    assertThat(normalizedDestinations.get(2).getWeight()).isEqualTo(50);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetNormalizedDestinationsUnder10() {
    List<TrafficRoutingDestination> destinations =
        List.of(TrafficRoutingDestination.builder().host("host1").weight(2).build(),
            TrafficRoutingDestination.builder().host("host2").weight(3).build(),
            TrafficRoutingDestination.builder().host("host3").weight(5).build());

    K8sTrafficRoutingConfig config = K8sTrafficRoutingConfig.builder().destinations(destinations).build();

    List<TrafficRoutingDestination> normalizedDestinations = config.getNormalizedDestinations();

    assertThat(normalizedDestinations.get(0).getHost()).isEqualTo("host1");
    assertThat(normalizedDestinations.get(0).getWeight()).isEqualTo(20);
    assertThat(normalizedDestinations.get(1).getHost()).isEqualTo("host2");
    assertThat(normalizedDestinations.get(1).getWeight()).isEqualTo(30);
    assertThat(normalizedDestinations.get(2).getHost()).isEqualTo("host3");
    assertThat(normalizedDestinations.get(2).getWeight()).isEqualTo(50);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetNormalizedDestinationsOver100() {
    List<TrafficRoutingDestination> destinations =
        List.of(TrafficRoutingDestination.builder().host("host1").weight(200).build(),
            TrafficRoutingDestination.builder().host("host2").weight(300).build(),
            TrafficRoutingDestination.builder().host("host3").weight(500).build());
    K8sTrafficRoutingConfig config = K8sTrafficRoutingConfig.builder().destinations(destinations).build();

    List<TrafficRoutingDestination> normalizedDestinations = config.getNormalizedDestinations();

    assertThat(normalizedDestinations.get(0).getHost()).isEqualTo("host1");
    assertThat(normalizedDestinations.get(0).getWeight()).isEqualTo(20);
    assertThat(normalizedDestinations.get(1).getHost()).isEqualTo("host2");
    assertThat(normalizedDestinations.get(1).getWeight()).isEqualTo(30);
    assertThat(normalizedDestinations.get(2).getHost()).isEqualTo("host3");
    assertThat(normalizedDestinations.get(2).getWeight()).isEqualTo(50);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetNormalizedDestinationsCustomNumber() {
    List<TrafficRoutingDestination> destinations =
        List.of(TrafficRoutingDestination.builder().host("host1").weight(1).build(),
            TrafficRoutingDestination.builder().host("host2").weight(3).build(),
            TrafficRoutingDestination.builder().host("host3").weight(4).build());
    K8sTrafficRoutingConfig config = K8sTrafficRoutingConfig.builder().destinations(destinations).build();

    List<TrafficRoutingDestination> normalizedDestinations = config.getNormalizedDestinations();

    assertThat(normalizedDestinations.get(0).getHost()).isEqualTo("host1");
    assertThat(normalizedDestinations.get(0).getWeight()).isEqualTo(12);
    assertThat(normalizedDestinations.get(1).getHost()).isEqualTo("host2");
    assertThat(normalizedDestinations.get(1).getWeight()).isEqualTo(37);
    assertThat(normalizedDestinations.get(2).getHost()).isEqualTo("host3");
    assertThat(normalizedDestinations.get(2).getWeight()).isEqualTo(50);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetNormalizedDestinationsWhenWeightNotProvided() {
    List<TrafficRoutingDestination> destinations =
        List.of(TrafficRoutingDestination.builder().host("host1").weight(0).build(),
            TrafficRoutingDestination.builder().host("host2").weight(0).build(),
            TrafficRoutingDestination.builder().host("host3").weight(0).build());
    K8sTrafficRoutingConfig config = K8sTrafficRoutingConfig.builder().destinations(destinations).build();

    List<TrafficRoutingDestination> normalizedDestinations = config.getNormalizedDestinations();

    assertThat(normalizedDestinations.get(0).getHost()).isEqualTo("host1");
    assertThat(normalizedDestinations.get(0).getWeight()).isEqualTo(33);
    assertThat(normalizedDestinations.get(1).getHost()).isEqualTo("host2");
    assertThat(normalizedDestinations.get(1).getWeight()).isEqualTo(33);
    assertThat(normalizedDestinations.get(2).getHost()).isEqualTo("host3");
    assertThat(normalizedDestinations.get(2).getWeight()).isEqualTo(33);
  }
}
