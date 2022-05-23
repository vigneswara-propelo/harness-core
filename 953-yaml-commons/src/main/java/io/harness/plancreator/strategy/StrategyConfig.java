package io.harness.plancreator.strategy;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.validation.OneOfField;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(PIPELINE)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OneOfField(fields = {"matrix", "for"})
@RecasterAlias("io.harness.plancreator.strategy.StrategyConfig")
public class StrategyConfig {
  @JsonProperty("matrix") MatrixConfig matrixConfig;
  @JsonProperty("for") HarnessForConfig forConfig;
}
