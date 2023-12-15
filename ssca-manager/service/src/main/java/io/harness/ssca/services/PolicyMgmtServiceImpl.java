/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.services;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.network.SafeHttpCall;
import io.harness.opaclient.OpaServiceClient;
import io.harness.opaclient.model.OpaEvaluationResponseHolder;
import io.harness.opaclient.model.OpaPolicyEvaluationResponse;
import io.harness.opaclient.model.OpaPolicySetEvaluationResponse;
import io.harness.serializer.JsonUtils;
import io.harness.ssca.beans.OpaPolicyEvaluationResult;
import io.harness.ssca.beans.Violation;
import io.harness.ssca.entities.NormalizedSBOMComponentEntity;
import io.harness.utils.PolicyEvalUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

@OwnedBy(HarnessTeam.SSCA)
@Slf4j
public class PolicyMgmtServiceImpl implements PolicyMgmtService {
  private static final String OPA_OUTPUT_JSON_KEY_0 = "expressions";
  private static final String OPA_OUTPUT_JSON_KEY_1 = "value";
  private static final String OPA_OUTPUT_JSON_KEY_2 = "sbom";
  private static final String OPA_OUTPUT_JSON_KEY_3 = "allow_list_violations";
  private static final String OPA_OUTPUT_JSON_KEY_4 = "deny_list_violations";
  @Inject OpaServiceClient opaServiceClient;

  @Override
  public OpaPolicyEvaluationResult evaluate(String accountId, String orgIdentifier, String projectIdentifier,
      List<String> policySetRef, List<NormalizedSBOMComponentEntity> normalizedSBOMComponentEntities) {
    OpaEvaluationResponseHolder opaEvaluationResponseHolder;
    try {
      Instant start = Instant.now();
      log.info("Starting evaluation of policy set {} against {} sbom components", policySetRef,
          normalizedSBOMComponentEntities.size());
      opaEvaluationResponseHolder = SafeHttpCall.executeWithExceptions(opaServiceClient.evaluateWithCredentialsByID(
          accountId, orgIdentifier, projectIdentifier, PolicyEvalUtils.getPolicySetsStringForQueryParam(policySetRef),
          PolicyEvalUtils.getEntityMetadataString("ssca_enforcement"),
          JsonUtils.asTree(normalizedSBOMComponentEntities)));
      log.info("Completed evaluation of policy set {} against {} sbom components", policySetRef,
          normalizedSBOMComponentEntities.size());
      Instant end = Instant.now();
      log.info("Time taken to evaluate {} sbom entities for policySets {} is {} ms",
          normalizedSBOMComponentEntities.size(), policySetRef, Duration.between(start, end).toMillis());
      List<Violation> allowListViolations = new ArrayList<>();
      List<Violation> denyListViolations = new ArrayList<>();

      List<OpaPolicySetEvaluationResponse> opaPolicySetEvaluationResponses = opaEvaluationResponseHolder.getDetails();
      if (CollectionUtils.isNotEmpty(opaPolicySetEvaluationResponses)) {
        for (OpaPolicySetEvaluationResponse opaPolicySetEvaluationResponse : opaPolicySetEvaluationResponses) {
          List<OpaPolicyEvaluationResponse> opaPolicyEvaluationResponses = opaPolicySetEvaluationResponse.getDetails();
          if (CollectionUtils.isNotEmpty(opaPolicyEvaluationResponses)) {
            for (OpaPolicyEvaluationResponse opaPolicyEvaluationResponse : opaPolicyEvaluationResponses) {
              populateViolationDetailsFromSinglePolicyEvaluationResponse(allowListViolations, denyListViolations,
                  opaPolicyEvaluationResponse, opaPolicySetEvaluationResponse.getName(),
                  opaPolicySetEvaluationResponse.getIdentifier());
            }
          }
        }
      }
      return OpaPolicyEvaluationResult.builder()
          .allowListViolations(allowListViolations)
          .denyListViolations(denyListViolations)
          .build();
    } catch (Exception ex) {
      log.error("Exception while evaluating OPA rules", ex);
      throw new InvalidRequestException("Exception while evaluating OPA rules: " + ex.getMessage(), ex);
    }
  }

  private static void populateViolationDetailsFromSinglePolicyEvaluationResponse(List<Violation> allowListViolations,
      List<Violation> denyListViolations, OpaPolicyEvaluationResponse singlePolicyEvaluationResponse,
      String policySetName, String policySetIdentifier) {
    JsonNode opaEvaluationEngineOutputNode = extractOpaEvaluationEngineOutputNode(singlePolicyEvaluationResponse);
    ArrayNode allowListViolationsNode = (ArrayNode) opaEvaluationEngineOutputNode.get(OPA_OUTPUT_JSON_KEY_3);
    allowListViolationsNode.forEach(allowListViolation
        -> extractAndPopulateViolation(allowListViolations, allowListViolation, policySetName, policySetIdentifier,
            singlePolicyEvaluationResponse.getPolicy().getName(),
            singlePolicyEvaluationResponse.getPolicy().getIdentifier()));
    ArrayNode denyListViolationsNode = (ArrayNode) opaEvaluationEngineOutputNode.get(OPA_OUTPUT_JSON_KEY_4);
    denyListViolationsNode.forEach(denyListViolation
        -> extractAndPopulateViolation(denyListViolations, denyListViolation, policySetName, policySetIdentifier,
            singlePolicyEvaluationResponse.getPolicy().getName(),
            singlePolicyEvaluationResponse.getPolicy().getIdentifier()));
  }

  private static JsonNode extractOpaEvaluationEngineOutputNode(
      OpaPolicyEvaluationResponse singlePolicyEvaluationResponse) {
    return JsonUtils.asTree(singlePolicyEvaluationResponse.getOutput())
        .get(0)
        .get(OPA_OUTPUT_JSON_KEY_0)
        .get(0)
        .get(OPA_OUTPUT_JSON_KEY_1)
        .get(OPA_OUTPUT_JSON_KEY_2);
  }

  private static void extractAndPopulateViolation(List<Violation> violations, JsonNode violationNode,
      String policySetName, String policySetIdentifier, String policyName, String policyIdentifier) {
    Violation violation = extractViolationFromJsonNode(violationNode);
    if (violation != null) {
      violations.add(violation.toBuilder()
                         .policySetName(policySetName)
                         .policySetIdentifier(policySetIdentifier)
                         .policyName(policyName)
                         .policyIdentifier(policyIdentifier)
                         .build());
    }
  }

  private static Violation extractViolationFromJsonNode(JsonNode jsonNode) {
    JsonNode violationNode = jsonNode.get(0);
    ArrayNode violatingArtifactUuidsNode = (ArrayNode) violationNode.get("violations");
    if (violatingArtifactUuidsNode.isEmpty()) {
      return null;
    }
    List<String> violatingArtifactUuids = new ArrayList<>();
    violatingArtifactUuidsNode.forEach(uuidNode -> violatingArtifactUuids.add(uuidNode.textValue()));
    return Violation.builder()
        .artifactUuids(violatingArtifactUuids)
        .type(violationNode.get("type").asText())
        .rule(getSanitisedRule(violationNode.get("rule")))
        .build();
  }

  private static JsonNode getSanitisedRule(JsonNode rule) {
    Iterator<JsonNode> it = rule.iterator();
    while (it.hasNext()) {
      JsonNode next = it.next();
      if (next.isObject() && next.isEmpty()) {
        it.remove();
      } else {
        getSanitisedRule(next);
      }
    }
    return rule;
  }
}
