package io.harness.plancreator.strategy;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(PIPELINE)
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RecasterAlias("io.harness.plancreator.strategy.HarnessForConfig")
public class HarnessForConfig {
  @JsonProperty("iteration") int iteration;
  @JsonProperty("batchSize") int batchSize;
}
