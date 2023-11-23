/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.utils;

import io.harness.pipeline.remote.PipelineServiceClient;
import io.harness.remote.client.NGRestUtils;
import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class PipelineUtils {
  @Inject PipelineServiceClient pipelineServiceClient;
  private static String PIPELINE_EXECUTION_SUMMARY = "pipelineExecutionSummary";
  private static String PIPELINE_NAME = "name";

  public JsonNode getPipelineExecutionSummaryResponse(String pipelineExecutionId, String accountId,
      String orgIdentifier, String projectIdentifier, String stageNodeId) {
    JsonNode rootNode = null;
    try {
      Object pmsExecutionSummary = NGRestUtils.getResponse(pipelineServiceClient.getExecutionDetailV2(
          pipelineExecutionId, accountId, orgIdentifier, projectIdentifier, stageNodeId));
      rootNode = JsonUtils.asTree(pmsExecutionSummary);
    } catch (Exception e) {
      log.error(String.format("PMS Request Failed. Exception: %s", e));
    }
    return rootNode;
  }

  public JsonNode getPipelineExecutionSummaryResponse(
      String pipelineExecutionId, String accountId, String orgIdentifier, String projectIdentifier) {
    JsonNode rootNode = null;
    try {
      Object pmsExecutionSummary = NGRestUtils.getResponse(
          pipelineServiceClient.getExecutionDetailV2(pipelineExecutionId, accountId, orgIdentifier, projectIdentifier));
      rootNode = JsonUtils.asTree(pmsExecutionSummary);
    } catch (Exception e) {
      log.error(String.format("PMS Request Failed. Exception: %s", e));
    }
    return rootNode;
  }

  public String parsePipelineName(JsonNode pmsExecutionSummaryNode) {
    if (pmsExecutionSummaryNode == null || pmsExecutionSummaryNode.get(PIPELINE_EXECUTION_SUMMARY) == null
        || pmsExecutionSummaryNode.get(PIPELINE_EXECUTION_SUMMARY).get(PIPELINE_NAME) == null) {
      return null;
    }
    return pmsExecutionSummaryNode.get(PIPELINE_EXECUTION_SUMMARY).get(PIPELINE_NAME).asText();
  }
}
