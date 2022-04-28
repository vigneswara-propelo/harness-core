package io.harness.ng.core.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Value
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CDP)
@Schema(
    name = "HostValidationParams", description = "Host validation parameters, including host names and delegate tags.")
public class HostValidationParams {
  @JsonProperty("hosts") @Schema(description = "Hosts to be validated", required = true) @NonNull List<String> hosts;
  @JsonProperty("tags") @Schema(description = "Delegate tags (optional)") List<String> tags;
}
