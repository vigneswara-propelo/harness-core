/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services.drift;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.spec.server.ssca.v1.model.ArtifactSbomDriftRequestBody;
import io.harness.spec.server.ssca.v1.model.ArtifactSbomDriftResponse;
import io.harness.spec.server.ssca.v1.model.OrchestrationDriftSummary;
import io.harness.ssca.beans.drift.ComponentDriftResults;
import io.harness.ssca.beans.drift.ComponentDriftStatus;
import io.harness.ssca.beans.drift.DriftBase;
import io.harness.ssca.beans.drift.LicenseDriftResults;
import io.harness.ssca.beans.drift.LicenseDriftStatus;

import org.springframework.data.domain.Pageable;

@OwnedBy(HarnessTeam.SSCA)
public interface SbomDriftService {
  ArtifactSbomDriftResponse calculateSbomDrift(
      String accountId, String orgId, String projectId, String artifactId, ArtifactSbomDriftRequestBody requestBody);

  ArtifactSbomDriftResponse calculateSbomDriftForOrchestration(
      String accountId, String orgId, String projectId, String orchestrationId, DriftBase driftBase);

  ComponentDriftResults getComponentDrifts(String accountId, String orgId, String projectId, String driftId,
      ComponentDriftStatus status, Pageable pageable, String searchTerm);

  LicenseDriftResults getLicenseDrifts(String accountId, String orgId, String projectId, String driftId,
      LicenseDriftStatus status, Pageable pageable, String searchTerm);

  OrchestrationDriftSummary getSbomDriftSummary(
      String accountId, String orgId, String projectId, String orchestrationId);
}
