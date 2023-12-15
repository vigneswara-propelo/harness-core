/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ticketserviceclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.network.Http;
import io.harness.sto.beans.entities.TicketServiceConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@OwnedBy(HarnessTeam.STO)
public class TicketServiceClientFactory implements Provider<TicketServiceClient> {
  private TicketServiceConfig serviceConfig;

  @Inject
  public TicketServiceClientFactory(TicketServiceConfig serviceConfig) {
    this.serviceConfig = serviceConfig;
  }

  @Override
  public TicketServiceClient get() {
    Gson gson = new GsonBuilder().setLenient().create();
    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(serviceConfig.getInternalUrl())
                            .client(Http.getUnsafeOkHttpClient(serviceConfig.getInternalUrl()))
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();
    return retrofit.create(TicketServiceClient.class);
  }
}
