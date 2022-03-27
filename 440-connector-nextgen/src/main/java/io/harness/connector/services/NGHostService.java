package io.harness.connector.services;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.pdcconnector.HostDTO;
import io.harness.delegate.beans.connector.pdcconnector.HostFilterDTO;
import io.harness.ng.beans.PageRequest;

import org.springframework.data.domain.Page;

@OwnedBy(CDP)
public interface NGHostService {
  Page<HostDTO> filterHostsByConnector(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, HostFilterDTO filter, PageRequest pageRequest);
}
