package io.harness.plancreator.strategy;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(PIPELINE)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@RecasterAlias("io.harness.plancreator.strategy.ExcludeConfig")
public class ExcludeConfig {
  Map<String, String> exclude;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public ExcludeConfig(Map<String, String> axisValue) {
    this.exclude = axisValue;
  }
}
