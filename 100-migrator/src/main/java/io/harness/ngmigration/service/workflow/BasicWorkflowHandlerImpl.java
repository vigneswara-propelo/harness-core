/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.workflow;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.ngmigration.beans.MigrationContext;
import io.harness.ngmigration.beans.WorkflowMigrationContext;

import software.wings.beans.Workflow;
import software.wings.service.impl.yaml.handler.workflow.BasicWorkflowYamlHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_MIGRATOR})
public class BasicWorkflowHandlerImpl extends WorkflowHandler {
  @Inject BasicWorkflowYamlHandler basicWorkflowYamlHandler;

  @Override
  public JsonNode getTemplateSpec(MigrationContext migrationContext, Workflow workflow) {
    return getDeploymentStageTemplateSpec(
        migrationContext, WorkflowMigrationContext.newInstance(migrationContext, workflow));
  }
}
