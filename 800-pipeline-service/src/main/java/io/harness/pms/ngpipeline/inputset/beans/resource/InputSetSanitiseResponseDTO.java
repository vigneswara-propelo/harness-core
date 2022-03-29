package io.harness.pms.ngpipeline.inputset.beans.resource;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@OwnedBy(PIPELINE)
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("InputSetSanitiseResponse")
@Schema(name = "InputSetSanitiseResponse", description = "This contains Input Set sanitise API Response")
public class InputSetSanitiseResponseDTO {
  @Schema(description = "This contains Input Set details after removing invalid fields.")
  InputSetResponseDTOPMS inputSetUpdateResponse;
  @Schema(description = "If true, it means the Input Set is providing no fields and should be deleted")
  boolean shouldDeleteInputSet;
}
