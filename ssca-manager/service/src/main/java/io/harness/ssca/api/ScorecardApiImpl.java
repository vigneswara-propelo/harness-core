/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.api;

import io.harness.spec.server.ssca.v1.ScorecardApi;
import io.harness.spec.server.ssca.v1.model.SaveResponse;
import io.harness.spec.server.ssca.v1.model.SbomScorecardRequestBody;
import io.harness.spec.server.ssca.v1.model.SbomScorecardResponseBody;
import io.harness.ssca.services.ScorecardService;

import com.google.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

public class ScorecardApiImpl implements ScorecardApi {
  @Inject ScorecardService scorecardService;
  @Override
  public Response getSbomScorecard(String org, String project, String orchestrateId, String harnessAccount) {
    SbomScorecardResponseBody response =
        scorecardService.getByOrchestrationId(harnessAccount, org, project, orchestrateId);

    return Response.ok(200).entity(response).build();
  }

  @Override
  public Response saveSbomScorecard(
      String org, String project, String orchestrateId, @Valid SbomScorecardRequestBody body, String harnessAccount) {
    scorecardService.save(body);
    return Response.ok().status(200).entity(new SaveResponse().status("SUCCESS")).build();
  }
}
