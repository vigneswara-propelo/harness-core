package io.harness.delegate.beans.ci.pod;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@JsonTypeName("EmptyDir")
public class EmptyDirVolume implements PodVolume {
  @NotNull private String name;
  @NotNull private String mountPath;
  String medium;
  Integer sizeMib;

  @Override
  public PodVolume.Type getType() {
    return Type.EMPTY_DIR;
  }
}
