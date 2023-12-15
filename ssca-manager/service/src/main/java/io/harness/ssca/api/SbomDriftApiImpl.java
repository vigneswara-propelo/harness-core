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
import io.harness.spec.server.ssca.v1.model.ComponentDrift;
import io.harness.spec.server.ssca.v1.model.LicenseDrift;
import io.harness.spec.server.ssca.v1.model.OrchestrationStepDriftRequestBody;
import io.harness.ssca.beans.drift.ComponentDriftResults;
import io.harness.ssca.beans.drift.ComponentDriftStatus;
import io.harness.ssca.beans.drift.DriftBase;
import io.harness.ssca.beans.drift.LicenseDriftResults;
import io.harness.ssca.beans.drift.LicenseDriftStatus;
import io.harness.ssca.mapper.SbomDriftMapper;
import io.harness.ssca.services.drift.SbomDriftService;
import io.harness.utils.ApiUtils;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
      @Min(0L) Integer page, @Min(1L) @Max(1000L) Integer limit, String searchTerm) {
    Pageable pageable = PageRequest.of(page, limit);
    ComponentDriftStatus componentDriftStatus = SbomDriftMapper.mapStatusToComponentDriftStatus(status);
    ComponentDriftResults componentDriftResults = sbomDriftService.getComponentDrifts(
        harnessAccount, org, project, drift, componentDriftStatus, pageable, searchTerm);
    ResponseBuilder responseBuilder = Response.ok();
    if (componentDriftResults != null) {
      List<ComponentDrift> componentDrifts =
          SbomDriftMapper.toComponentDriftResponseList(componentDriftResults.getComponentDrifts());
      responseBuilder.entity(componentDrifts);
      ApiUtils.addLinksHeader(responseBuilder, componentDriftResults.getTotalComponentDrifts(), page, limit);
    } else {
      responseBuilder.entity(new ArrayList<>());
      ApiUtils.addLinksHeader(responseBuilder, 0, page, limit);
    }
    return responseBuilder.build();
  }

  @Override
  public Response getLicenseDrift(String org, String project, String drift, String harnessAccount, String status,
      @Min(0L) Integer page, @Min(1L) @Max(1000L) Integer limit, String searchTerm) {
    Pageable pageable = PageRequest.of(page, limit);
    LicenseDriftStatus licenseDriftStatus = SbomDriftMapper.mapStatusToLicenseDriftStatus(status);
    LicenseDriftResults licenseDriftResults = sbomDriftService.getLicenseDrifts(
        harnessAccount, org, project, drift, licenseDriftStatus, pageable, searchTerm);
    List<LicenseDrift> licenseDrifts =
        SbomDriftMapper.toLicenseDriftResponseList(licenseDriftResults.getLicenseDrifts());
    ResponseBuilder responseBuilder = Response.ok().entity(licenseDrifts);
    ApiUtils.addLinksHeader(responseBuilder, licenseDriftResults.getTotalLicenseDrifts(), page, limit);
    return responseBuilder.build();
  }

  @Override
  public Response calculateDriftForOrchestrationStep(String org, String project, String orchestration,
      @Valid OrchestrationStepDriftRequestBody body, String harnessAccount) {
    DriftBase driftBase = SbomDriftMapper.getDriftBase(body);
    ArtifactSbomDriftResponse responseBody =
        sbomDriftService.calculateSbomDriftForOrchestration(harnessAccount, org, project, orchestration, driftBase);
    return Response.ok().entity(responseBody).build();
  }
}
