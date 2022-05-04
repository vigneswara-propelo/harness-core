/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.template.refresh;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.common.NGExpressionUtils.matchesInputSetPattern;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.template.beans.NGTemplateConstants.DUMMY_NODE;
import static io.harness.template.beans.NGTemplateConstants.TEMPLATE_INPUTS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ngexception.NGTemplateException;
import io.harness.pms.merger.YamlConfig;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.RuntimeInputFormHelper;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.template.beans.yaml.NGTemplateConfig;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.helpers.TemplateMergeServiceHelper;
import io.harness.template.services.NGTemplateService;
import io.harness.utils.YamlPipelineUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDC)

/**
 * Class containing methods related to the refreshTemplateInputs API only
 * Aim -> to have the refresh code together in a separate class rather than in the TemplateMergeServiceImpl
 */
public class RefreshTemplateInputs {
  @Inject NGTemplateService templateService;

  private TemplateMergeServiceHelper templateMergeServiceHelper;

  // Returns the refreshed YAML when a YAML String is passed.
  public String refreshTemplates(String accountId, String orgId, String projectId, String yaml) {
    // Case -> empty YAML, cannot refresh
    if (isEmpty(yaml)) {
      throw new NGTemplateException("Yaml to be refreshed cannot be empty.");
    }

    YamlNode yamlNode;
    try {
      // Parsing the YAML to get the YamlNode
      yamlNode = YamlUtils.readTree(yaml).getNode();
    } catch (IOException e) {
      log.error("Could not convert yaml to JsonNode. Yaml:\n" + yaml, e);
      throw new NGTemplateException("Could not convert yaml to JsonNode: " + e.getMessage());
    }

    // TemplateCache Map
    Map<String, TemplateEntity> templateCacheMap = new HashMap<>();

    // Updated ResMap -> Key,Value pairs of the YAML with Refreshed Template Inputs
    Map<String, Object> resMap = getResMap(accountId, orgId, projectId, yamlNode, templateCacheMap);

    // Returning the Refreshed YAML corresponding to the ResMap
    String refreshedYaml = "";
    try {
      refreshedYaml = YamlPipelineUtils.getYamlString(resMap);
    } catch (JsonProcessingException e) {
      log.error("Could not convert to yaml");
    }
    return refreshedYaml;
  }

  // Gets the Updated ResMap -> Key,Value pairs of the YAML with Refreshed Template Inputs
  private Map<String, Object> getResMap(String accountId, String orgId, String projectId, YamlNode yamlNode,
      Map<String, TemplateEntity> templateCacheMap) {
    Map<String, Object> resMap = new LinkedHashMap<>();

    // Iterating over the YAML fields to go to all the Templates Present
    for (YamlField childYamlField : yamlNode.fields()) {
      String fieldName = childYamlField.getName();
      JsonNode value = childYamlField.getNode().getCurrJsonNode();
      boolean isTemplatePresent = templateMergeServiceHelper.isTemplatePresent(fieldName, value);

      // If Template is present, Refresh the Template Inputs
      if (isTemplatePresent) {
        // Updated JsonNode with Refreshed TemplateInputs
        value = getUpdatedTemplateValue(accountId, orgId, projectId, value, templateCacheMap);
      }

      if (value.isValueNode() || YamlUtils.checkIfNodeIsArrayWithPrimitiveTypes(value)) {
        // Value -> LeafNode
        resMap.put(fieldName, value);
      } else if (value.isArray()) {
        // Value -> Array
        resMap.put(
            fieldName, getResMapInArray(accountId, orgId, projectId, childYamlField.getNode(), templateCacheMap));
      } else {
        // Value -> Object
        resMap.put(fieldName, getResMap(accountId, orgId, projectId, childYamlField.getNode(), templateCacheMap));
      }
    }
    return resMap;
  }

  // Gets the ResMap if the yamlNode is of the type Array
  private List<Object> getResMapInArray(String accountId, String orgId, String projectId, YamlNode yamlNode,
      Map<String, TemplateEntity> templateCacheMap) {
    List<Object> arrayList = new ArrayList<>();

    // Iterate over the array
    for (YamlNode arrayElement : yamlNode.asArray()) {
      if (yamlNode.getCurrJsonNode().isValueNode()) {
        // Value -> LeafNode
        arrayList.add(arrayElement);
      } else if (arrayElement.isArray()) {
        // Value -> Array
        arrayList.add(getResMapInArray(accountId, orgId, projectId, yamlNode, templateCacheMap));
      } else {
        // Value -> Object
        arrayList.add(getResMap(accountId, orgId, projectId, arrayElement, templateCacheMap));
      }
    }
    return arrayList;
  }

  // Gets the Updated Template Input values
  private JsonNode getUpdatedTemplateValue(String accountId, String orgId, String projectId, JsonNode TemplateNodeValue,
      Map<String, TemplateEntity> templateCacheMap) {
    // Template Inputs linked to the YAML
    JsonNode templateInputs = TemplateNodeValue.get(TEMPLATE_INPUTS);

    // Template YAML corresponding to the TemplateRef and Version Label
    TemplateEntity templateEntity = templateMergeServiceHelper.getLinkedTemplateEntity(
        accountId, orgId, projectId, TemplateNodeValue, templateCacheMap);
    String templateYaml = templateEntity.getYaml();

    // Generate the Template Spec from the Template YAML
    JsonNode templateSpec;
    try {
      NGTemplateConfig templateConfig = YamlPipelineUtils.read(templateYaml, NGTemplateConfig.class);
      templateSpec = templateConfig.getTemplateInfoConfig().getSpec();
    } catch (IOException e) {
      log.error("Could not read template yaml", e);
      throw new NGTemplateException("Could not read template yaml: " + e.getMessage());
    }

    // YAML config with Refreshed values
    YamlConfig yamlConfig = getTemplateYamlConfig(templateInputs, templateSpec);

    ObjectNode updatedValue = (ObjectNode) TemplateNodeValue;

    if (yamlConfig == null) {
      // CASE -> When Template does not contain any runtime inputs
      updatedValue.remove(TEMPLATE_INPUTS);
    } else {
      // Generating the JsonNode corresponding to the Refreshed Template Spec YAML
      JsonNode finalNode = yamlConfig.getYamlMap();

      // Removing the Dummy Node added to the Template Inputs
      finalNode = finalNode.get(DUMMY_NODE);

      // Inserting the Updated Value of TemplateInputs corresponding to the TemplateInputs field
      updatedValue.set(TEMPLATE_INPUTS, finalNode);
    }

    // Returning the Refreshed Template Inputs Value
    return updatedValue;
  }

  // Gets the yaml config corresponding to the Updated TemplateSpec FQN-Map
  private YamlConfig getTemplateYamlConfig(JsonNode templateInputs, JsonNode templateSpec) {
    // TemplateSpecYAML for Runtime Inputs of Template
    Map<String, JsonNode> dummyTemplateSpecMap = new LinkedHashMap<>();
    dummyTemplateSpecMap.put(DUMMY_NODE, templateSpec);
    String dummyTemplateSpecYaml = null;
    try {
      dummyTemplateSpecYaml = YamlPipelineUtils.getYamlString(dummyTemplateSpecMap);
    } catch (JsonProcessingException e) {
      log.error("Could not convert to yaml");
    }
    String templateSpecYaml = RuntimeInputFormHelper.createTemplateFromYaml(dummyTemplateSpecYaml);

    if (templateSpecYaml == null) {
      return null;
    }

    // LinkedTemplateYAML with Template Inputs
    Map<String, JsonNode> dummyTemplateInputsMap = new LinkedHashMap<>();
    dummyTemplateInputsMap.put(DUMMY_NODE, templateInputs);
    String dummyTemplateInputsYaml = null;
    try {
      dummyTemplateInputsYaml = YamlPipelineUtils.getYamlString(dummyTemplateInputsMap);
    } catch (JsonProcessingException e) {
      log.error("Could not convert to yaml");
    }

    // Refreshed TemplateSpec FQN-Map
    Map<FQN, Object> templateSpecMap = getUpdatedTemplateInputsFqnMap(dummyTemplateInputsYaml, templateSpecYaml);

    JsonNode templateSpecYamlNode = null;
    try {
      templateSpecYamlNode = YamlUtils.readTree(templateSpecYaml).getNode().getCurrJsonNode();
    } catch (Exception e) {
      log.error("Error while reading the Template YAML");
    }

    // Returning the YAML config from the FQN-Map and the TemplateSpecYAML
    return new YamlConfig(templateSpecMap, templateSpecYamlNode);
  }

  // Updates the Template Inputs FQN Map by replacing the values there in the TemplateSpec YAML
  private Map<FQN, Object> getUpdatedTemplateInputsFqnMap(String inputsYaml, String templateSpecYaml) {
    // TemplateSpecYAML ->  FQN Map
    YamlConfig templateSpecConfig = new YamlConfig(templateSpecYaml);
    Map<FQN, Object> templateSpecMap = templateSpecConfig.getFqnToValueMap();

    // TemplateInputYAML -> FQN Map
    YamlConfig originalYamlConfig = new YamlConfig(inputsYaml);
    Map<FQN, Object> originalYamlFQNMap = new LinkedHashMap<>(originalYamlConfig.getFqnToValueMap());

    // Iterating all the Runtime Inputs in the TemplateSpec FQN-Map and replacing the updated values of the runtime
    // inputs with those in the TemplateInputs FQN-Map
    Set<FQN> keySet = templateSpecMap.keySet();
    keySet.forEach(key -> {
      if (originalYamlFQNMap.containsKey(key)) {
        Object value = originalYamlFQNMap.get(key);
        if (matchesInputSetPattern(templateSpecMap.get(key).toString())) {
          templateSpecMap.replace(key, value);
        }
      }
    });

    // Returning the Refreshed TemplateSpec FQN-Map
    return templateSpecMap;
  }
}
