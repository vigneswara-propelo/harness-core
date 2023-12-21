/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.ticket;

import io.harness.ssca.beans.ticket.TicketRequestDto;
import io.harness.ssca.beans.ticket.TicketResponseDto;

import com.google.inject.Inject;
import java.io.IOException;

public class TicketServiceRestClientServiceImpl implements TicketServiceRestClientService {
  @Inject private TicketServiceRestClient ticketServiceRestClient;
  @Inject private RequestExecutor requestExecutor;
  @Override
  public TicketResponseDto createTicket(
      String accountId, String orgId, String projectId, TicketRequestDto ticketRequestDto) throws IOException {
    return requestExecutor.execute(ticketServiceRestClient.createTicket(accountId, projectId, orgId, ticketRequestDto));
  }
}