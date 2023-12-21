/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ipallowlist;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.spec.server.ng.v1.model.IPAllowlistConfigValidateResponse;

import retrofit2.Call;
import retrofit2.http.*;

@OwnedBy(PL)
public interface IPAllowListClient {
  String IP_ALLOW_LIST_BASEURL = "/v1/ip-allowlist";

  @GET(IP_ALLOW_LIST_BASEURL + "/allowed/ip-address")
  Call<IPAllowlistConfigValidateResponse> allowedIpAddress(
      @Header("Harness-Account") String accountId, @Query("ip_address") String ipAddress);
}
