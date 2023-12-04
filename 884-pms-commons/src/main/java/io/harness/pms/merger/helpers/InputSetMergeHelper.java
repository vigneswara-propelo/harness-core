/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.merger.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.jackson.JsonNodeUtils;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.yaml.HarnessYamlVersion;
import io.harness.pms.yaml.NGYamlHelper;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@UtilityClass
@Slf4j
public class InputSetMergeHelper {
  // TODO(shalini): remove older methods with yaml string once all are moved to jsonNode
  public String mergeInputSetIntoPipeline(
      String pipelineYaml, String inputSetPipelineCompYaml, boolean appendInputSetValidator) {
    return MergeHelper.mergeRuntimeInputValuesIntoOriginalYaml(
        pipelineYaml, inputSetPipelineCompYaml, appendInputSetValidator);
  }

  public String mergeInputSetIntoPipelineForGivenStages(String pipelineYaml, String inputSetPipelineCompYaml,
      boolean appendInputSetValidator, List<String> stageIdentifiers) {
    return MergeHelper.mergeRuntimeInputValuesIntoOriginalYaml(
        pipelineYaml, removeNonRequiredStages(inputSetPipelineCompYaml, stageIdentifiers), appendInputSetValidator);
  }

  public JsonNode mergeInputSets(
      JsonNode template, List<JsonNode> inputSetJsonNodeList, boolean appendInputSetValidator) {
    return mergeInputSetsForGivenStages(template, inputSetJsonNodeList, appendInputSetValidator, null);
  }

  public JsonNode mergeInputSetsForGivenStages(JsonNode template, List<JsonNode> inputSetJsonNodeList,
      boolean appendInputSetValidator, List<String> stageIdentifiers) {
    List<JsonNode> inputSetPipelineCompJsonNodeList =
        getInputSetPipelineCompJsonNodeListWithJsonNode(inputSetJsonNodeList)
            .stream()
            .map(o -> removeNonRequiredStages(o, stageIdentifiers))
            .collect(Collectors.toList());
    Collections.reverse(inputSetPipelineCompJsonNodeList);
    return MergeHelper.mergeRuntimeInputValuesIntoOriginalJsonNode(
        template, inputSetPipelineCompJsonNodeList, appendInputSetValidator);
  }

  private List<JsonNode> getInputSetPipelineCompJsonNodeListWithJsonNode(List<JsonNode> inputSetJsonNodeList) {
    return inputSetJsonNodeList.stream()
        .map(jsonNode -> {
          try {
            return InputSetYamlHelper.getPipelineComponent(jsonNode);
          } catch (InvalidRequestException e) {
            return jsonNode;
          }
        })
        .collect(Collectors.toList());
  }

  public JsonNode mergeInputSetsV1(List<JsonNode> inputSetJsonNodeList) {
    if (EmptyPredicate.isEmpty(inputSetJsonNodeList)) {
      return null;
    }
    JsonNode mergedInputSetNode = getMergedInputSetNodeWithJsonNode(inputSetJsonNodeList);
    return mergedInputSetNode != null ? mergedInputSetNode : JsonUtils.readTree("{}");
  }

  private JsonNode getMergedInputSetNodeWithJsonNode(List<JsonNode> inputSetJsonNodeList) {
    JsonNode mergedInputSetNode = null;
    for (JsonNode inputSetNode : inputSetJsonNodeList) {
      if (mergedInputSetNode == null) {
        mergedInputSetNode = inputSetNode;
      } else {
        JsonNodeUtils.merge(mergedInputSetNode, inputSetNode);
      }
    }
    return mergedInputSetNode;
  }

  public JsonNode removeNonRequiredStages(JsonNode inputSetPipelineCompJsonNode, List<String> stageIdentifiers) {
    Map<FQN, Object> inputSetFQNMap = FQNMapGenerator.generateFQNMap(inputSetPipelineCompJsonNode);
    if (EmptyPredicate.isNotEmpty(stageIdentifiers)) {
      FQNHelper.removeNonRequiredStages(inputSetFQNMap, stageIdentifiers);
    }
    return YamlMapGenerator.generateYamlMap(inputSetFQNMap, inputSetPipelineCompJsonNode, false);
  }

  public JsonNode removeNonRequiredStagesV1(JsonNode mergedRuntimeInputYaml, List<String> stageIdentifiers) {
    JsonNode stages = mergedRuntimeInputYaml.get(YAMLFieldNameConstants.SPEC).get(YAMLFieldNameConstants.STAGES);
    if (stages instanceof ArrayNode) {
      ArrayNode stagesArray = (ArrayNode) stages;
      Iterator<JsonNode> it = stagesArray.iterator();
      while (it.hasNext()) {
        JsonNode stage = it.next();
        if (stage.get(YAMLFieldNameConstants.TYPE).asText().equals(YAMLFieldNameConstants.PARALLEL)) {
          JsonNode childStages = stage.get(YAMLFieldNameConstants.SPEC).get(YAMLFieldNameConstants.STAGES);
          if (childStages instanceof ArrayNode) {
            ArrayNode childStagesArray = (ArrayNode) childStages;
            Iterator<JsonNode> iterator = childStagesArray.iterator();
            while (iterator.hasNext()) {
              JsonNode childStage = iterator.next();
              removeStagesFromJsonNode(iterator, stageIdentifiers, childStage);
            }
          }
          if (stage.get(YAMLFieldNameConstants.SPEC).get(YAMLFieldNameConstants.STAGES).isEmpty()) {
            it.remove();
          }
        } else {
          removeStagesFromJsonNode(it, stageIdentifiers, stage);
        }
      }
    }
    return mergedRuntimeInputYaml;
  }

  private void removeStagesFromJsonNode(Iterator<JsonNode> it, List<String> stageIdentifiers, JsonNode stage) {
    if (!stage.has(YAMLFieldNameConstants.ID)) {
      it.remove();
      return;
    }
    if (!stageIdentifiers.contains(stage.get(YAMLFieldNameConstants.ID).asText())) {
      it.remove();
    }
  }

  public String removeNonRequiredStages(String mergedRuntimeInputYaml, List<String> stageIdentifiers) {
    if (HarnessYamlVersion.V0.equals(NGYamlHelper.getVersion(mergedRuntimeInputYaml))) {
      return YamlUtils.writeYamlString(
          removeNonRequiredStages(YamlUtils.readAsJsonNode(mergedRuntimeInputYaml), stageIdentifiers));
    }
    return YamlUtils.writeYamlString(
        removeNonRequiredStagesV1(YamlUtils.readAsJsonNode(mergedRuntimeInputYaml), stageIdentifiers));
  }
}
