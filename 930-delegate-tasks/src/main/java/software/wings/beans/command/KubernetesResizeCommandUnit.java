/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.command;

import io.harness.container.ContainerInfo;
import io.harness.exception.UnsupportedOperationException;

import software.wings.api.ContainerServiceData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by brett on 3/3/17
 */
@JsonTypeName("RESIZE_KUBERNETES")
public class KubernetesResizeCommandUnit extends ContainerResizeCommandUnit {
  public KubernetesResizeCommandUnit() {
    super(CommandUnitType.RESIZE_KUBERNETES);
  }

  @Override
  protected List<ContainerInfo> executeResize(
      ContextData contextData, ContainerServiceData containerServiceData, ExecutionLogCallback executionLogCallback) {
    throw new UnsupportedOperationException(
        String.format("Command Unit: %s is no longer supported. Please contact harness customer care.",
            CommandUnitType.KUBERNETES_SETUP));
  }

  @Override
  protected void postExecution(
      ContextData contextData, List<ContainerServiceData> allData, ExecutionLogCallback executionLogCallback) {
    throw new UnsupportedOperationException(
        String.format("Command Unit: %s is no longer supported. Please contact harness customer care.",
            CommandUnitType.KUBERNETES_SETUP));
  }

  @Override
  protected Map<String, Integer> getActiveServiceCounts(ContextData contextData) {
    return Collections.emptyMap();
  }

  @Override
  protected Map<String, String> getActiveServiceImages(ContextData contextData) {
    return Collections.emptyMap();
  }

  @Override
  protected Optional<Integer> getServiceDesiredCount(ContextData contextData) {
    return Optional.empty();
  }

  @Override
  protected Map<String, Integer> getTrafficWeights(ContextData contextData) {
    return new HashMap<>();
  }

  @Override
  protected int getPreviousTrafficPercent(ContextData contextData) {
    return 0;
  }

  @Override
  protected Integer getDesiredTrafficPercent(ContextData contextData) {
    return ((KubernetesResizeParams) contextData.resizeParams).getTrafficPercent();
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("RESIZE_KUBERNETES")
  public static class Yaml extends ContainerResizeCommandUnit.Yaml {
    public Yaml() {
      super(CommandUnitType.RESIZE_KUBERNETES.name());
    }

    @Builder
    public Yaml(String name, String deploymentType) {
      super(name, CommandUnitType.RESIZE_KUBERNETES.name(), deploymentType);
    }
  }
}
