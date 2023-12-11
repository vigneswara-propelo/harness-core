/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s;

import static io.harness.exception.WingsException.USER;

import static java.lang.String.format;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.exception.InvalidRequestException;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_K8S})
public class K8sApiVersion {
  String group;
  String version;

  public static K8sApiVersion fromApiVersion(String apiVersion) {
    String[] apiVersionArray = apiVersion.split("/");
    if (apiVersionArray.length != 2) {
      throw new InvalidRequestException(
          format("Api version for virtual service is invalid. ApiVersion: %s", apiVersion), USER);
    }
    return new K8sApiVersion(apiVersionArray[0], apiVersionArray[1]);
  }
}
