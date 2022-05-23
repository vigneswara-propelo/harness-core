package io.harness.plancreator.strategy;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.beans.SwaggerConstants.STRING_LIST_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.list;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(PIPELINE)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@RecasterAlias("io.harness.plancreator.strategy.AxisConfig")
@JsonRootName("")
public class AxisConfig {
  @NotNull
  @YamlSchemaTypes(value = {string, list})
  @ApiModelProperty(dataType = STRING_LIST_CLASSPATH)
  ParameterField<List<String>> axisValue;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public AxisConfig(ParameterField<List<String>> axisValue) {
    this.axisValue = axisValue;
  }
}
