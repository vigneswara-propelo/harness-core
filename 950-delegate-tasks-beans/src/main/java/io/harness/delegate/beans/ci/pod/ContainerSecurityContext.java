package io.harness.delegate.beans.ci.pod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ContainerSecurityContext {
  Boolean allowPrivilegeEscalation;
  Boolean privileged;
  String procMount;
  Boolean readOnlyRootFilesystem;
  Boolean runAsNonRoot;
  Integer runAsGroup;
  Integer runAsUser;
  ContainerCapabilities capabilities;
}
