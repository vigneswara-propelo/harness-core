/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.command;

import static io.harness.annotations.dev.HarnessModule._930_DELEGATE_TASKS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.UnsupportedOperationException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.dto.SettingAttribute;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by brett on 3/3/17
 */
@Slf4j
@JsonTypeName("KUBERNETES_SETUP")
@OwnedBy(CDP)
@TargetModule(_930_DELEGATE_TASKS)
public class KubernetesSetupCommandUnit extends ContainerSetupCommandUnit {
  public KubernetesSetupCommandUnit() {
    super(CommandUnitType.KUBERNETES_SETUP);
    throw new UnsupportedOperationException(
        String.format("Command Unit: %s is no longer supported. Please contact harness customer care.",
            CommandUnitType.KUBERNETES_SETUP));
  }

  @Override
  protected CommandExecutionStatus executeInternal(CommandExecutionContext context,
      SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> edd, ContainerSetupParams containerSetupParams,
      Map<String, String> serviceVariables, Map<String, String> safeDisplayServiceVariables,
      ExecutionLogCallback executionLogCallback) {
    throw new UnsupportedOperationException(
        String.format("Command Unit: %s is no longer supported. Please contact harness customer care.",
            CommandUnitType.KUBERNETES_SETUP));
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("KUBERNETES_SETUP")
  public static class Yaml extends ContainerSetupCommandUnit.Yaml {
    public Yaml() {
      super(CommandUnitType.KUBERNETES_SETUP.name());
    }

    @Builder
    public Yaml(String name, String deploymentType) {
      super(name, CommandUnitType.KUBERNETES_SETUP.name(), deploymentType);
    }
  }
}
