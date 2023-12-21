/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.ssca.ticket;

import io.harness.remote.client.ServiceHttpClientConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class TicketServiceRestClientModule extends AbstractModule {
  ServiceHttpClientConfig ticketServiceRestClientConfig;

  public TicketServiceRestClientModule(ServiceHttpClientConfig ticketServiceRestClientConfig) {
    this.ticketServiceRestClientConfig = ticketServiceRestClientConfig;
  }

  @Override
  protected void configure() {
    bind(TicketServiceRestClient.class)
        .toProvider(new TicketServiceRestClientFactory(ticketServiceRestClientConfig))
        .in(Singleton.class);
  }
}