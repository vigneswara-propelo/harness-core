/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import software.wings.security.annotations.ExternalFacingApiAuth;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Api(value = "/ng/accounts")
@Path("/ng/accounts")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._955_ACCOUNT_MGMT)
public class TrustLevelResourceNG {
  private final AccountService accountService;

  @GET
  @Path("/trustLevel")
  @ExternalFacingApiAuth
  public RestResponse<Integer> getAccountTrustLevel(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(accountService.getTrustLevel(accountId));
  }

  @GET
  @Path("/update-trust-level")
  @ApiOperation(value = "Used to update the trust level", hidden = true)
  public RestResponse<Boolean> updateAccountTrustLevel(
      @QueryParam("accountId") String accountId, @QueryParam("trustLevel") Integer trustLevel) {
    return new RestResponse<>(accountService.updateTrustLevel(accountId, trustLevel));
  }
}
