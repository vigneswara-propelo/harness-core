/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.k8s.trafficrouting;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import java.util.List;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
public class TrafficRoutingCommon {
  private final int K8S_RESOURCE_NAME_MAX = 253;
  private final String STABLE_PLACE_HOLDER = "stable";
  private final String STAGE_PLACE_HOLDER = "stage";
  private final String CANARY_PLACE_HOLDER = "canary";

  public String updatePlaceHoldersIfExist(String host, String stable, String stage) {
    if (isEmpty(host)) {
      throw new InvalidArgumentsException("Host must be specified in the destination for traffic routing");
    } else if (STABLE_PLACE_HOLDER.equals(host) && isNotEmpty(stable)) {
      return stable;
    } else if ((STAGE_PLACE_HOLDER.equals(host) || CANARY_PLACE_HOLDER.equals(host)) && isNotEmpty(stage)) {
      return stage;
    } else {
      return host;
    }
  }

  public String getTrafficRoutingResourceName(String name, String suffix, String defaultName) {
    return name != null ? StringUtils.truncate(name, K8S_RESOURCE_NAME_MAX - suffix.length()) + suffix : defaultName;
  }

  public String getApiVersion(Set<String> clusterAvailableApis, List<String> providerApis, String defaultVersion,
      String providerName, LogCallback logCallback) {
    return providerApis.stream().filter(clusterAvailableApis::contains).findFirst().orElseGet(() -> {
      logCallback.saveExecutionLog(
          format(
              "Supported API version wasn't found in the cluster for %s provider. Default version %s will be used for resource creation",
              providerName, defaultVersion),
          LogLevel.WARN, CommandExecutionStatus.RUNNING);
      return defaultVersion;
    });
  }
}
