/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.yaml.individualschema;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STAGE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STAGES;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEPS;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.template.utils.TemplateSchemaFetcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashSet;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@Slf4j
@OwnedBy(PIPELINE)
@Singleton
public abstract class BaseTemplateSchemaParser extends AbstractStaticSchemaParser {
  @Inject TemplateSchemaFetcher templateSchemaFetcher;
  @Override
  void checkIfRootNodeAndAddIntoFqnToNodeMap(String currentFqn, String childNodeRefValue, ObjectNode objectNode) {}

  abstract void findRootNodesAndInitialiseSchema();
  abstract String getYamlVersion();
  @Override
  void init() {
    JsonNode rootSchemaNode = templateSchemaFetcher.getStaticYamlSchema(getYamlVersion());
    rootSchemaJsonNode = rootSchemaNode;
    // Populating the template schema in the nodeToResolvedSchemaMap with rootSchemaNode because we already have the
    // complete template schema so no need to calculate.
    nodeToResolvedSchemaMap.put(YAMLFieldNameConstants.TEMPLATE, (ObjectNode) rootSchemaNode);
    traverseNodeAndExtractAllRefsRecursively(rootSchemaJsonNode, "/#");
    findRootNodesAndInitialiseSchema();
  }

  @Override
  IndividualSchemaGenContext getIndividualSchemaGenContext() {
    return IndividualSchemaGenContext.builder()
        .rootSchemaNode(rootSchemaJsonNode)
        .resolvedFqnSet(new HashSet<>())
        .build();
  }
  @Override
  public JsonNode getFieldNode(InputFieldMetadata inputFieldMetadata) {
    String fqnFromParentNode = inputFieldMetadata.getFqnFromParentNode();
    // TODO Refactor: Use TemplateSchemaMetadata and create TemplateSchemaRequest instead of using
    // PipelineSchemaRequest.
    PipelineSchemaRequest schemaRequest =
        PipelineSchemaRequest.builder()
            .individualSchemaMetadata(
                PipelineSchemaMetadata.builder()
                    .nodeGroup(getFormattedNodeGroup(inputFieldMetadata.getParentTypeOfNodeGroup()))
                    .nodeType(inputFieldMetadata.getParentNodeType())
                    .build())
            .fqnFromParentNode(fqnFromParentNode)
            .build();

    return super.getFieldNode(schemaRequest);
  }

  private String getFormattedNodeGroup(String nodeGroup) {
    if (STEPS.equals(nodeGroup)) {
      return STEP;
    }
    if (STAGES.equals(nodeGroup)) {
      return STAGE;
    }
    return nodeGroup;
  }

  @Override
  Boolean checkIfParserReinitializationNeeded() {
    if (templateSchemaFetcher.useSchemaFromHarnessSchemaRepo()) {
      // We will reinitialise the individual schema in 15 min for stress env or for env where
      // useSchemaFromHarnessSchemaRepo is enabled (dev-space/local)
      return System.currentTimeMillis() - lastInitializedTime >= MAX_TIME_TO_REINITIALIZE_PARSER;
    }
    return false;
  }
}