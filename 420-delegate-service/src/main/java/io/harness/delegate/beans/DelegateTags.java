package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Value;

@Value
@OwnedBy(HarnessTeam.DEL)
public class DelegateTags {
  List<String> tags;
}
