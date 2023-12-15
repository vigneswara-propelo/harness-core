/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.cdng.k8s.trafficrouting.DefaultK8sTrafficRouting;
import io.harness.cdng.manifest.yaml.K8sStepCommandFlag;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@TypeAlias("k8sCanaryStepParameters")
@RecasterAlias("io.harness.cdng.k8s.K8sCanaryStepParameters")
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_K8S})
public class K8sCanaryStepParameters extends K8sCanaryBaseStepInfo implements K8sSpecParameters {
  @Builder(builderMethodName = "infoBuilder")
  public K8sCanaryStepParameters(InstanceSelectionWrapper instanceSelection, ParameterField<Boolean> skipDryRun,
      ParameterField<List<TaskSelectorYaml>> delegateSelectors, List<K8sStepCommandFlag> commandFlags,
      DefaultK8sTrafficRouting trafficRouting) {
    super(instanceSelection, skipDryRun, delegateSelectors, commandFlags, trafficRouting);
  }

  @NotNull
  @Override
  public List<String> getCommandUnits() {
    List<String> commandUnits = K8sSpecParameters.super.getCommandUnits();
    if (this.trafficRouting != null) {
      commandUnits.add(3, K8sCommandUnitConstants.TrafficRouting);
    }
    return commandUnits;
  }
}
