/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.yaml.schema.inputs;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.pms.yaml.YamlSchemaFieldConstants.DEPENDS_ON;
import static io.harness.pms.yaml.YamlSchemaFieldConstants.INPUT_PROPERTIES;
import static io.harness.pms.yaml.YamlSchemaFieldConstants.INTERNAL_TYPE;
import static io.harness.pms.yaml.YamlSchemaFieldConstants.METADATA;
import static io.harness.pms.yaml.YamlSchemaFieldConstants.TYPE;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.jackson.JsonNodeUtils;
import io.harness.pms.merger.YamlConfig;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.yaml.individualschema.InputFieldMetadata;
import io.harness.yaml.individualschema.SchemaParserInterface;
import io.harness.yaml.schema.inputs.beans.FixedValueDependencyDetails;
import io.harness.yaml.schema.inputs.beans.InputDetails;
import io.harness.yaml.schema.inputs.beans.InputMetadata;
import io.harness.yaml.schema.inputs.beans.RuntimeInputDependencyDetails;
import io.harness.yaml.schema.inputs.beans.YamlInputDetails;
import io.harness.yaml.utils.JsonFieldUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class InputsSchemaServiceImpl implements InputsSchemaService {
  // TODO(BRIJESH): Check for more optimisations.
  @Override
  public List<YamlInputDetails> getInputsSchemaRelations(SchemaParserInterface schemaParser, String yaml) {
    YamlConfig yamlConfig = new YamlConfig(yaml);
    List<YamlInputDetails> finalYamlInputDetails = new ArrayList<>();
    Map<InputFieldMetadata, String> specFieldToInputNameMap = new HashMap<>();
    Map<InputDetails, List<InputFieldMetadata>> inputToSpecFieldDependencyMap = new HashMap<>();

    List<InputDetails> inputDetailsList = YamlInputUtils.getYamlInputList(yaml);
    Map<Set<String>, InputDetails> yamlInputExpressionToYamlInputMap =
        YamlInputUtils.prepareYamlInputExpressionToYamlInputMap(inputDetailsList);
    Map<String, List<FQN>> FQNsForAllInputs = YamlInputUtils.parseFQNsForAllInputsInYaml(
        yamlConfig.getFqnToValueMap(), yamlInputExpressionToYamlInputMap.keySet());

    yamlInputExpressionToYamlInputMap.forEach((inputExpressionList, inputDetails) -> {
      List<FQN> FQNList = new ArrayList<>();
      inputExpressionList.forEach(
          inputExpression -> FQNList.addAll(FQNsForAllInputs.getOrDefault(inputExpression, Collections.emptyList())));
      InputMetadata inputMetadata = null;
      if (isNotEmpty(FQNList)) {
        inputMetadata = new InputMetadata();
        for (FQN fqn : FQNList) {
          // fetch corresponding input-details for the given template yaml
          String parentNodeType = YamlInputUtils.getParentNodeTypeForGivenFQNField(yamlConfig.getYamlMap(), fqn);
          InputFieldMetadata inputFieldMetadata =
              InputFieldMetadata.builder().parentNodeType(parentNodeType).fqn(fqn).build();
          specFieldToInputNameMap.put(inputFieldMetadata, inputDetails.getName());
          JsonNode inputFieldSchemaNode = schemaParser.getFieldNode(inputFieldMetadata);
          JsonNode metadataSchemaNode = JsonFieldUtils.get(inputFieldSchemaNode, METADATA);
          if (metadataSchemaNode != null) {
            handleMetadataField(metadataSchemaNode, fqn, inputMetadata, inputFieldMetadata,
                inputToSpecFieldDependencyMap, inputDetails);
          } else if (inputFieldSchemaNode.has(YAMLFieldNameConstants.DOLLAR_REF)) {
            JsonNode rootSchema = schemaParser.getRootSchemaJsonNode();
            JsonNode referencedNode = JsonNodeUtils.goToPath(
                rootSchema, inputFieldSchemaNode.get(YAMLFieldNameConstants.DOLLAR_REF).asText());
            JsonNode classMetadataSchemaNode = JsonFieldUtils.get(referencedNode, METADATA);
            if (classMetadataSchemaNode != null) {
              handleMetadataField(classMetadataSchemaNode, fqn, inputMetadata, inputFieldMetadata,
                  inputToSpecFieldDependencyMap, inputDetails);
            }
          }
        }
      }
      finalYamlInputDetails.add(
          YamlInputDetails.builder().inputDetails(inputDetails).inputMetadata(inputMetadata).build());
    });

    finalYamlInputDetails.forEach(yamlInputDetails -> {
      List<InputFieldMetadata> requiredFieldsMetadata =
          inputToSpecFieldDependencyMap.get(yamlInputDetails.getInputDetails());
      if (requiredFieldsMetadata != null) {
        requiredFieldsMetadata.forEach(requiredFieldMetadata -> {
          if (specFieldToInputNameMap.containsKey(requiredFieldMetadata)) {
            yamlInputDetails.getInputMetadata().getDependencyDetails().addRuntimeInputDependency(
                RuntimeInputDependencyDetails.builder()
                    .fieldName(requiredFieldMetadata.getFieldName())
                    .inputName(specFieldToInputNameMap.get(requiredFieldMetadata))
                    .build());
          } else {
            JsonNode fieldNode = schemaParser.getFieldNode(requiredFieldMetadata);
            yamlInputDetails.getInputMetadata().getDependencyDetails().addFixedValueDependency(
                FixedValueDependencyDetails.builder()
                    .propertyName(requiredFieldMetadata.getFieldName())
                    .propertyType(JsonFieldUtils.getText(fieldNode, TYPE))
                    .fieldValue(yamlConfig.getFqnToValueMap().get(requiredFieldMetadata.getFqn()))
                    .build());
          }
        });
      }
    });

    return finalYamlInputDetails;
  }

  private void handleMetadataField(JsonNode metadataSchemaNode, FQN fqn, InputMetadata inputMetadata,
      InputFieldMetadata inputFieldMetadata, Map<InputDetails, List<InputFieldMetadata>> inputToSpecFieldDependencyMap,
      InputDetails inputDetails) {
    JsonNode inputPropertiesNode = JsonFieldUtils.get(metadataSchemaNode, INPUT_PROPERTIES);
    if (inputPropertiesNode != null) {
      // Assuming inputType is always text field, check if validation is required
      inputMetadata.addInputDetailsPerField(JsonFieldUtils.getTextOrEmpty(inputPropertiesNode, TYPE),
          JsonFieldUtils.getTextOrEmpty(inputPropertiesNode, INTERNAL_TYPE));
      if (JsonFieldUtils.isPresent(inputPropertiesNode, DEPENDS_ON)) {
        ArrayNode requiredFields = JsonFieldUtils.getArrayNode(inputPropertiesNode, DEPENDS_ON);
        List<InputFieldMetadata> requiredFieldsMetadata = new ArrayList<>();
        requiredFields.forEach(requiredField
            -> requiredFieldsMetadata.add(InputFieldMetadata
                                              .builder()
                                              // Assuming dependency is always on a field from same step
                                              .parentNodeType(inputFieldMetadata.getParentNodeType())
                                              .fqn(fqn.getSiblingFQN(requiredField.asText()))
                                              .build()));
        inputToSpecFieldDependencyMap.put(inputDetails, requiredFieldsMetadata);
      }
    }
  }
}
