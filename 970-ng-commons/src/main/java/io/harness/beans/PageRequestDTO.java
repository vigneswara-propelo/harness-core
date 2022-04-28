package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PL)
// this is serializable dto used for serialization purposes between the manager and the delegate.
public class PageRequestDTO {
  int pageIndex;
  int pageSize;
}
