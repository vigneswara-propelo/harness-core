package io.harness.delegate.beans.ci.pod;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@JsonTypeName("PersistentVolumeClaim")
public class PVCVolume implements PodVolume {
  @NotNull private String name;
  @NotNull private String mountPath;
  @NotNull private String claimName;
  Boolean readOnly;

  @Override
  public PodVolume.Type getType() {
    return Type.PVC;
  }
}
