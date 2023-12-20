/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.manifestspec;

import static io.harness.ngtriggers.conditionchecker.ConditionOperator.REGEX;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngtriggers.beans.source.artifact.ManifestTypeSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;

import software.wings.beans.trigger.ManifestTriggerCondition;
import software.wings.beans.trigger.Trigger;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_MIGRATOR})
@OwnedBy(HarnessTeam.CDC)
public interface ManifestSpecMapper {
  ManifestTypeSpec getTriggerSpec(Map<CgEntityId, CgEntityNode> entities, ConnectorInfoDTO helmConnector,
      Map<CgEntityId, NGYamlFile> migratedEntities, Trigger trigger);

  default List<TriggerEventDataCondition> getEventConditions(Trigger trigger) {
    List<TriggerEventDataCondition> eventConditions = new ArrayList<>(Collections.emptyList());
    if (trigger.getCondition() != null) {
      ManifestTriggerCondition triggerCondition = (ManifestTriggerCondition) trigger.getCondition();
      if (triggerCondition.getVersionRegex() != null) {
        TriggerEventDataCondition triggerEventDataCondition = TriggerEventDataCondition.builder()
                                                                  .key("version")
                                                                  .operator(REGEX)
                                                                  .value(triggerCondition.getVersionRegex())
                                                                  .build();
        eventConditions.add(triggerEventDataCondition);
      }
    }
    return eventConditions;
  }
}
