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
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TrafficRoutingCommonTest extends CategoryTest {
  private static final String STABLE_SERVICE = "stable-service-name";
  public static final String STAGE_SERVICE = "stage-service-name";
  @Mock LogCallback logCallback;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testUpdatePlaceHolderWhenStable() {
    assertThat(TrafficRoutingCommon.updatePlaceHoldersIfExist("stable", STABLE_SERVICE, STAGE_SERVICE))
        .isEqualTo(STABLE_SERVICE);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testUpdatePlaceHolderWhenStage() {
    assertThat(TrafficRoutingCommon.updatePlaceHoldersIfExist("stage", STABLE_SERVICE, STAGE_SERVICE))
        .isEqualTo(STAGE_SERVICE);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testUpdatePlaceHolderWhenCanary() {
    assertThat(TrafficRoutingCommon.updatePlaceHoldersIfExist("canary", STABLE_SERVICE, STAGE_SERVICE))
        .isEqualTo(STAGE_SERVICE);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testUpdatePlaceHolderWithoutStableOrStage() {
    assertThat(TrafficRoutingCommon.updatePlaceHoldersIfExist("some-host", null, null)).isEqualTo("some-host");
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testUpdatePlaceHolderWhenEmptyHost() {
    TrafficRoutingCommon.updatePlaceHoldersIfExist(null, STABLE_SERVICE, STAGE_SERVICE);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testUpdatePlaceHolderStableWithoutStableService() {
    assertThat(TrafficRoutingCommon.updatePlaceHoldersIfExist("stable", null, null)).isEqualTo("stable");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testUpdatePlaceHolderStageWithoutStageService() {
    assertThat(TrafficRoutingCommon.updatePlaceHoldersIfExist("stage", null, null)).isEqualTo("stage");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testUpdatePlaceHolderCanaryWithoutStageService() {
    assertThat(TrafficRoutingCommon.updatePlaceHoldersIfExist("canary", null, null)).isEqualTo("canary");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingResourceNameWhenNull() {
    assertThat(TrafficRoutingCommon.getTrafficRoutingResourceName(null, "-some-suffix", "defaultName"))
        .isEqualTo("defaultName");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingResourceName() {
    assertThat(TrafficRoutingCommon.getTrafficRoutingResourceName("resource-name", "-some-suffix", "defaultName"))
        .isEqualTo("resource-name-some-suffix");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetTrafficRoutingResourceWithOverLimitResourceName() {
    String overLimitResourceName = RandomStringUtils.randomAlphabetic(260);
    String result =
        TrafficRoutingCommon.getTrafficRoutingResourceName(overLimitResourceName, "-some-suffix", "defaultName");
    assertThat(result).endsWith("-some-suffix").hasSize(253);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetApiVersion() {
    Set<String> availableApis = Set.of("api1", "api2", "api3", "api4");
    List<String> providerApis = List.of("dummy1", "dummy2", "api2", "api4", "api5");
    String defaultApi = "api5";
    String result = TrafficRoutingCommon.getApiVersion(availableApis, providerApis, defaultApi, null, logCallback);
    assertThat(result).isEqualTo("api2");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testGetApiVersionDefault() {
    Set<String> availableApis = Set.of("api1", "api2", "api3");
    List<String> providerApis = List.of("api4", "api5");
    String defaultApi = "api6";
    String result = TrafficRoutingCommon.getApiVersion(availableApis, providerApis, defaultApi, null, logCallback);
    assertThat(result).isEqualTo("api6");
  }
}
