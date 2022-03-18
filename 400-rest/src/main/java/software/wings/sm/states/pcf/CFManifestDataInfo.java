package software.wings.sm.states.pcf;

import io.harness.pcf.model.ManifestType;

import software.wings.helpers.ext.k8s.request.K8sValuesLocation;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CFManifestDataInfo {
  private Map<K8sValuesLocation, Map<ManifestType, List<String>>> manifestMap;
  private String applicationManifestFilePath;
  private String autoscalarManifestFilePath;
}
