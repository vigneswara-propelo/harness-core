package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Data;

@Data
@OwnedBy(HarnessTeam.DEL)
public class DelegateApprovalResponse {
  private final String responseMessage;
  private final List<String> listOfUpdatedDelegateIds;
}
