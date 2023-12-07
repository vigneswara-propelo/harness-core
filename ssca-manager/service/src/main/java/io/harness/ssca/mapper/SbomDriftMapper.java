/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.mapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.spec.server.ssca.v1.model.ComponentDrift;
import io.harness.spec.server.ssca.v1.model.ComponentDriftResponse;
import io.harness.spec.server.ssca.v1.model.ComponentSummary;
import io.harness.ssca.beans.drift.ComponentDriftStatus;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.SSCA)
@UtilityClass
public class SbomDriftMapper {
  public ComponentDriftStatus mapStatusToComponentDriftStatus(String status) {
    switch (status) {
      case "added":
        return ComponentDriftStatus.ADDED;
      case "modified":
        return ComponentDriftStatus.MODIFIED;
      case "deleted":
        return ComponentDriftStatus.DELETED;
      case "all":
        return null;
      default:
        throw new InvalidRequestException("status could only be one of added / modified / deleted");
    }
  }

  public ComponentDriftResponse toComponentDriftResponse(String artifactName, String baseTag, String tag,
      List<io.harness.ssca.beans.drift.ComponentDrift> componentDrifts) {
    List<ComponentDrift> componentDriftsResponseList = new ArrayList<>();
    for (io.harness.ssca.beans.drift.ComponentDrift componentDrift : componentDrifts) {
      componentDriftsResponseList.add(toComponentDriftResponse(componentDrift));
    }
    return new ComponentDriftResponse()
        .componentDrifts(componentDriftsResponseList)
        .tag(tag)
        .baseTag(baseTag)
        .artifactName(artifactName);
  }

  private ComponentDrift toComponentDriftResponse(io.harness.ssca.beans.drift.ComponentDrift componentDrift) {
    if (componentDrift == null || componentDrift.getStatus() == null) {
      return null;
    }
    return new ComponentDrift()
        .newComponent(toComponentSummaryResponse(componentDrift.getNewComponent()))
        .oldComponent(toComponentSummaryResponse(componentDrift.getOldComponent()))
        .status(componentDrift.getStatus().toString());
  }

  private ComponentSummary toComponentSummaryResponse(io.harness.ssca.beans.drift.ComponentSummary componentSummary) {
    if (componentSummary == null) {
      return null;
    }
    return new ComponentSummary()
        .packageName(componentSummary.getPackageName())
        .packageVersion(componentSummary.getPackageVersion())
        .packageLicense(componentSummary.getPackageLicense().toString())
        .purl(componentSummary.getPurl())
        .packageSupplier(componentSummary.getPackageSupplierName())
        .packageManager(componentSummary.getPackageManager());
  }
}
