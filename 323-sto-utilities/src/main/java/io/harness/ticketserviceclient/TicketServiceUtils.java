/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ticketserviceclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;
import io.harness.sto.beans.entities.TicketServiceConfig;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Response;

@Getter
@Setter
@Slf4j
@Singleton
@OwnedBy(HarnessTeam.STO)
public class TicketServiceUtils {
  private static final String DEFAULT_PAGE_SIZE = "100";
  private final TicketServiceClient ticketServiceClient;
  private final TicketServiceConfig serviceConfig;

  @Inject
  public TicketServiceUtils(TicketServiceClient ticketServiceClient, TicketServiceConfig serviceConfig) {
    this.ticketServiceClient = ticketServiceClient;
    this.serviceConfig = serviceConfig;
  }

  @NotNull
  public String getTicketServiceToken(String accountId) {
    log.info("Initiating token request to Ticket service: {}", this.serviceConfig.getInternalUrl());
    JsonObject responseBody;
    if (accountId == null) {
      responseBody = makeAPICall(ticketServiceClient.generateTokenAllAccounts(this.serviceConfig.getGlobalToken()));
    } else {
      responseBody = makeAPICall(ticketServiceClient.generateToken(accountId, this.serviceConfig.getGlobalToken()));
    }

    if (responseBody.has("token")) {
      return responseBody.get("token").getAsString();
    }

    log.error("Response from Ticket service doesn't contain token information: {}", responseBody);

    return "";
  }

  @NotNull
  public String deleteAccountData(String accountId) {
    String token = getTicketServiceToken(null);
    String accessToken = "ApiKey " + token;

    return makeAPICall(ticketServiceClient.deleteAccountData(accessToken, accountId)).get("status").toString();
  }

  private JsonObject makeAPICall(Call<JsonObject> apiCall) {
    Response<JsonObject> response = null;
    try {
      response = apiCall.execute();
    } catch (IOException e) {
      throw new GeneralException("API request to Ticket service call failed", e);
    }

    // Received error from the server
    if (!response.isSuccessful()) {
      String errorBody = null;
      try {
        errorBody = response.errorBody().string();
      } catch (IOException e) {
        log.error("Could not read error body {}", response.errorBody());
      }

      throw new GeneralException(String.format(
          "API call to Ticket service failed. status code = %s, message = %s, response = %s", response.code(),
          response.message() == null ? "null" : response.message(), response.errorBody() == null ? "null" : errorBody));
    }

    if (response.body() == null) {
      throw new GeneralException("Cannot complete API call to Ticket service. Response body is null");
    }

    return response.body();
  }
}
