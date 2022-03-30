package io.harness.delegate.beans.ci.pod;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ContainerCapabilities {
  List<String> add;
  List<String> drop;
}
