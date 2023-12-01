/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.spec.server.ssca.v1.SbomDriftApi;
import io.harness.spec.server.ssca.v1.model.ComponentDriftResponse;
import io.harness.ssca.beans.drift.ComponentDrift;
import io.harness.ssca.beans.drift.ComponentDriftStatus;
import io.harness.ssca.mapper.SbomDriftMapper;
import io.harness.ssca.services.ArtifactService;
import io.harness.ssca.services.drift.SbomDriftService;

import com.google.inject.Inject;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.core.Response;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@OwnedBy(HarnessTeam.SSCA)
public class SbomDriftApiImpl implements SbomDriftApi {
  @Inject SbomDriftService sbomDriftService;
  @Inject ArtifactService artifactService;

  @Override
  public Response getComponentDrift(String org, String project, String artifact, String harnessAccount, String baseTag,
      String tag, String status, @Min(0L) Integer page, @Min(1L) @Max(1000L) Integer limit) {
    Pageable pageable = PageRequest.of(page, limit);
    ComponentDriftStatus componentDriftStatus = SbomDriftMapper.mapStatusToComponentDriftStatus(status);
    String artifactName = artifactService.getArtifactName(harnessAccount, org, project, artifact);
    if (EmptyPredicate.isEmpty(artifactName)) {
      throw new InvalidRequestException("Could not find artifact with artifact ID: " + artifact);
    }
    sbomDriftService.calculateDrift(harnessAccount, org, project, artifact, baseTag, tag);
    List<ComponentDrift> componentDrifts = sbomDriftService.getComponentDriftsByArtifactId(
        harnessAccount, org, project, artifact, baseTag, tag, componentDriftStatus, pageable);
    ComponentDriftResponse componentDriftResponse =
        SbomDriftMapper.toComponentDriftResponse(artifactName, baseTag, tag, componentDrifts);
    return Response.ok().entity(componentDriftResponse).build();
  }
}
