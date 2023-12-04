/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.api;

import io.harness.annotations.SSCAServiceAuth;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.spec.server.ssca.v1.BaselineApi;
import io.harness.spec.server.ssca.v1.model.BaselineRequestBody;
import io.harness.spec.server.ssca.v1.model.BaselineResponseBody;
import io.harness.spec.server.ssca.v1.model.SaveResponse;
import io.harness.ssca.beans.BaselineDTO;
import io.harness.ssca.services.BaselineService;

import com.google.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@OwnedBy(HarnessTeam.SSCA)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@SSCAServiceAuth
public class BaselineApiImpl implements BaselineApi {
  @Inject BaselineService baselineService;
  @Override
  public Response getBaselineForArtifact(String org, String project, String artifact, String harnessAccount) {
    BaselineDTO baselineDTO = baselineService.getBaselineForArtifact(harnessAccount, org, project, artifact);

    BaselineResponseBody response =
        new BaselineResponseBody().artifactId(baselineDTO.getArtifactId()).tag(baselineDTO.getTag());
    return Response.ok().entity(response).build();
  }

  @Override
  public Response setBaselineForArtifact(
      String org, String project, String artifact, @Valid BaselineRequestBody body, String harnessAccount) {
    boolean response = baselineService.setBaselineForArtifact(harnessAccount, org, project, artifact, body.getTag());
    return Response.ok().entity(new SaveResponse().status(response ? "SUCCESS" : "FAILURE")).build();
  }
}
