package io.harness.beans.yaml.extended.volumes;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.beans.SwaggerConstants.BOOLEAN_CLASSPATH;
import static io.harness.beans.SwaggerConstants.STRING_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("PersistentVolumeClaim")
@TypeAlias("persistentVolumeClaim")
@OwnedBy(CI)
public class PersistentVolumeClaimYaml implements CIVolume {
  @Builder.Default @NotNull private CIVolume.Type type = Type.PERSISTENT_VOLUME_CLAIM;
  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> mountPath;
  @NotNull private PersistentVolumeClaimYamlSpec spec;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PersistentVolumeClaimYamlSpec {
    @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> claimName;

    @YamlSchemaTypes({runtime})
    @ApiModelProperty(dataType = BOOLEAN_CLASSPATH)
    private ParameterField<Boolean> readOnly;
  }
}
