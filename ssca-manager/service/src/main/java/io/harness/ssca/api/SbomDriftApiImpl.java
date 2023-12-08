/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.spec.server.ssca.v1.SbomDriftApi;
import io.harness.spec.server.ssca.v1.model.ArtifactSbomDriftRequestBody;
import io.harness.spec.server.ssca.v1.model.ArtifactSbomDriftResponse;
import io.harness.ssca.services.drift.SbomDriftService;

import com.google.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.core.Response;

@OwnedBy(HarnessTeam.SSCA)
public class SbomDriftApiImpl implements SbomDriftApi {
  @Inject SbomDriftService sbomDriftService;

  @Override
  public Response calculateDriftForArtifact(
      String org, String project, String artifact, @Valid ArtifactSbomDriftRequestBody body, String harnessAccount) {
    if (body == null) {
      throw new InvalidRequestException("Request body cannot be null");
    }
    ArtifactSbomDriftResponse responseBody =
        sbomDriftService.calculateSbomDrift(harnessAccount, org, project, artifact, body);
    return Response.ok().entity(responseBody).build();
  }

  @Override
  public Response getComponentDrift(String org, String project, String drift, String harnessAccount, String status,
      @Min(0L) Integer page, @Min(1L) @Max(1000L) Integer limit) {
    return null;
  }

  @Override
  public Response getLicenseDrift(String org, String project, String drift, String harnessAccount, String status,
      @Min(0L) Integer page, @Min(1L) @Max(1000L) Integer limit) {
    return null;
  }
}
