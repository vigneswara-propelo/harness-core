/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.backstage.resources;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.annotations.IdpServiceAuthIfHasApiKey;
import io.harness.idp.backstage.service.BackstageService;
import io.harness.idp.common.IdpCommonService;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.spec.server.idp.v1.BackstageHarnessApi;
import io.harness.spec.server.idp.v1.model.BackstageHarnessSyncEntitiesResponse;
import io.harness.spec.server.idp.v1.model.BackstageHarnessSyncRequest;

import com.google.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@NextGenManagerAuth
@Slf4j
@OwnedBy(HarnessTeam.IDP)
public class BackstageHarnessApiImpl implements BackstageHarnessApi {
  private static final String SYNC_SUCCESS_RESPONSE = "SUCCESS";
  private static final String SYNC_ERROR_RESPONSE = "ERROR";

  private IdpCommonService idpCommonService;
  private BackstageService backstageService;

  @Override
  public Response backstageHarnessSyncAllAccounts() {
    idpCommonService.checkUserAuthorization();
    backstageService.sync();
    return Response.status(Response.Status.OK)
        .entity(new BackstageHarnessSyncEntitiesResponse().status(SYNC_SUCCESS_RESPONSE))
        .build();
  }

  @Override
  public Response backstageHarnessSyncForAccount(@AccountIdentifier String harnessAccount) {
    idpCommonService.checkUserAuthorization();
    boolean result = backstageService.sync(harnessAccount);
    return responseOnResult(result);
  }

  @Override
  @IdpServiceAuthIfHasApiKey
  public Response backstageHarnessSyncForAccountEntity(
      @Valid BackstageHarnessSyncRequest body, @AccountIdentifier String harnessAccount) {
    boolean result = backstageService.sync(
        harnessAccount, body.getEntityIdentifier(), body.getAction().value(), body.getSyncMode().value());
    return responseOnResult(result);
  }

  private Response responseOnResult(boolean result) {
    if (result) {
      return Response.status(Response.Status.OK)
          .entity(new BackstageHarnessSyncEntitiesResponse().status(SYNC_SUCCESS_RESPONSE))
          .build();
    } else {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new BackstageHarnessSyncEntitiesResponse().status(SYNC_ERROR_RESPONSE))
          .build();
    }
  }
}
