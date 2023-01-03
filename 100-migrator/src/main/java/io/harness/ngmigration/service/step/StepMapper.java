/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.step;

import io.harness.cdng.pipeline.CdAbstractStepNode;
import io.harness.data.structure.CollectionUtils;
import io.harness.ngmigration.beans.NGYamlFile;
import io.harness.ngmigration.service.MigratorUtility;
import io.harness.plancreator.steps.AbstractStepNode;
import io.harness.plancreator.steps.internal.PmsAbstractStepNode;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.timeout.Timeout;

import software.wings.beans.GraphNode;
import software.wings.ngmigration.CgEntityId;
import software.wings.sm.State;

import java.util.Map;

public interface StepMapper {
  int DEFAULT_TIMEOUT_MILLI = 600000;

  String getStepType(GraphNode stepYaml);

  State getState(GraphNode stepYaml);

  AbstractStepNode getSpec(Map<CgEntityId, NGYamlFile> migratedEntities, GraphNode graphNode);

  boolean areSimilar(GraphNode stepYaml1, GraphNode stepYaml2);

  default ParameterField<Timeout> getTimeout(GraphNode stepYaml) {
    Map<String, Object> properties = getProperties(stepYaml);

    String timeoutString = "10m";
    if (properties.containsKey("timeoutMillis")) {
      long t = Long.parseLong(properties.get("timeoutMillis").toString()) / 1000;
      timeoutString = t + "s";
    }
    return ParameterField.createValueField(Timeout.builder().timeoutString(timeoutString).build());
  }

  default ParameterField<Timeout> getTimeout(State state) {
    return MigratorUtility.getTimeout(state.getTimeoutMillis());
  }

  default String getDescription(GraphNode stepYaml) {
    Map<String, Object> properties = getProperties(stepYaml);
    return properties.getOrDefault("description", "").toString();
  }

  default Map<String, Object> getProperties(GraphNode stepYaml) {
    return CollectionUtils.emptyIfNull(stepYaml.getProperties());
  }

  default void baseSetup(GraphNode graphNode, AbstractStepNode stepNode) {
    stepNode.setIdentifier(MigratorUtility.generateIdentifier(graphNode.getName()));
    stepNode.setName(graphNode.getName());
    stepNode.setDescription(getDescription(graphNode));
    if (stepNode instanceof PmsAbstractStepNode) {
      PmsAbstractStepNode pmsAbstractStepNode = (PmsAbstractStepNode) stepNode;
      pmsAbstractStepNode.setTimeout(getTimeout(graphNode));
    }
    if (stepNode instanceof CdAbstractStepNode) {
      CdAbstractStepNode cdAbstractStepNode = (CdAbstractStepNode) stepNode;
      cdAbstractStepNode.setTimeout(getTimeout(graphNode));
    }
  }

  default void baseSetup(State state, AbstractStepNode stepNode) {
    stepNode.setIdentifier(MigratorUtility.generateIdentifier(state.getName()));
    stepNode.setName(state.getName());
    if (stepNode instanceof PmsAbstractStepNode) {
      PmsAbstractStepNode pmsAbstractStepNode = (PmsAbstractStepNode) stepNode;
      pmsAbstractStepNode.setTimeout(getTimeout(state));
    }
    if (stepNode instanceof CdAbstractStepNode) {
      CdAbstractStepNode cdAbstractStepNode = (CdAbstractStepNode) stepNode;
      cdAbstractStepNode.setTimeout(getTimeout(state));
    }
  }
}
