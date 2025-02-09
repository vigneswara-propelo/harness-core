/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.ssca.services.remediation_tracker;

import io.harness.spec.server.ssca.v1.model.ExcludeArtifactRequestBody;
import io.harness.spec.server.ssca.v1.model.RemediationTrackerCreateRequestBody;
import io.harness.spec.server.ssca.v1.model.RemediationTrackersOverallSummaryResponseBody;
import io.harness.ssca.entities.remediation_tracker.RemediationTrackerEntity;

public interface RemediationTrackerService {
  String createRemediationTracker(
      String accountId, String orgId, String projectId, RemediationTrackerCreateRequestBody body);

  boolean close(String accountId, String orgId, String projectId, String remediationTrackerId);

  boolean excludeArtifact(
      String accountId, String orgId, String projectId, String remediationTrackerId, ExcludeArtifactRequestBody body);

  void updateArtifactsAndEnvironments(RemediationTrackerEntity remediationTracker);

  RemediationTrackerEntity getRemediationTracker(String remediationTrackerId);

  RemediationTrackersOverallSummaryResponseBody getOverallSummaryForRemediationTrackers(
      String accountId, String orgId, String projectId);
}
