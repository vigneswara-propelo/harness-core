/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.ssca.api;

import io.harness.spec.server.ssca.v1.RemediationApi;
import io.harness.spec.server.ssca.v1.model.ExcludeArtifactRequestBody;
import io.harness.spec.server.ssca.v1.model.RemediationTrackerCreateRequestBody;
import io.harness.spec.server.ssca.v1.model.RemediationTrackerCreateResponseBody;
import io.harness.spec.server.ssca.v1.model.RemediationTrackersOverallSummaryResponseBody;
import io.harness.spec.server.ssca.v1.model.SaveResponse;
import io.harness.ssca.services.remediation_tracker.RemediationTrackerService;

import com.google.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

public class RemediationTrackerApiImpl implements RemediationApi {
  @Inject RemediationTrackerService remediationTrackerService;

  @Override
  public Response createRemediationTracker(
      String orgId, String projectId, @Valid RemediationTrackerCreateRequestBody body, String harnessAccount) {
    String remediationTrackerId =
        remediationTrackerService.createRemediationTracker(harnessAccount, orgId, projectId, body);
    RemediationTrackerCreateResponseBody response = new RemediationTrackerCreateResponseBody().id(remediationTrackerId);
    return Response.ok().entity(response).build();
  }

  @Override
  public Response close(String org, String project, String remediation, String harnessAccount) {
    boolean response = remediationTrackerService.close(harnessAccount, org, project, remediation);
    return Response.ok().entity(new SaveResponse().status(response ? "SUCCESS" : "FAILURE")).build();
  }

  @Override
  public Response excludeArtifact(
      String org, String project, String remediation, @Valid ExcludeArtifactRequestBody body, String harnessAccount) {
    boolean response = remediationTrackerService.excludeArtifact(harnessAccount, org, project, remediation, body);
    return Response.ok().entity(new SaveResponse().status(response ? "SUCCESS" : "FAILURE")).build();
  }

  @Override
  public Response getOverallSummary(String org, String project, String harnessAccount) {
    RemediationTrackersOverallSummaryResponseBody response =
        remediationTrackerService.getOverallSummaryForRemediationTrackers(harnessAccount, org, project);
    return Response.ok().entity(response).build();
  }
}
