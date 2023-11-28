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
import io.harness.serializer.JsonUtils;
import io.harness.ssca.beans.OpaPolicyEvaluationResult;
import io.harness.ssca.beans.Violation;
import io.harness.ssca.entities.NormalizedSBOMComponentEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.SSCA)
@Slf4j
public class PolicyMgmtServiceImpl implements PolicyMgmtService {
  private static final String OPA_OUTPUT_JSON_KEY_0 = "expressions";
  private static final String OPA_OUTPUT_JSON_KEY_1 = "value";
  private static final String OPA_OUTPUT_JSON_KEY_2 = "sbom";
  private static final String OPA_OUTPUT_JSON_KEY_3 = "allow_list_violations";
  private static final String OPA_OUTPUT_JSON_KEY_4 = "deny_list_violations";
  private static final int POLICY_SET_INDEX = 0;
  @Inject OpaServiceClient opaServiceClient;

  @Override
  public OpaPolicyEvaluationResult evaluate(String accountId, String orgIdentifier, String projectIdentifier,
      String policySetRef, List<NormalizedSBOMComponentEntity> normalizedSBOMComponentEntities) {
    OpaEvaluationResponseHolder opaEvaluationResponseHolder;
    try {
      log.info("Starting evaluation of policy set {} against {} sbom components", policySetRef,
          normalizedSBOMComponentEntities.size());
      opaEvaluationResponseHolder =
          SafeHttpCall.executeWithExceptions(opaServiceClient.evaluateWithCredentialsByID(accountId, orgIdentifier,
              projectIdentifier, policySetRef, null, JsonUtils.asTree(normalizedSBOMComponentEntities)));
      log.info("Completed evaluation of policy set {} against {} sbom components", policySetRef,
          normalizedSBOMComponentEntities.size());
      List<OpaPolicyEvaluationResponse> opaPolicyEvaluationResponses =
          opaEvaluationResponseHolder.getDetails().get(POLICY_SET_INDEX).getDetails();
      List<Violation> allowListViolations = new ArrayList<>();
      List<Violation> denyListViolations = new ArrayList<>();
      for (OpaPolicyEvaluationResponse singlePolicyEvaluationResponse : opaPolicyEvaluationResponses) {
        populateViolationDetailsFromSinglePolicyEvaluationResponse(
            allowListViolations, denyListViolations, singlePolicyEvaluationResponse);
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
      List<Violation> denyListViolations, OpaPolicyEvaluationResponse singlePolicyEvaluationResponse) {
    JsonNode opaEvaluationEngineOutputNode = extractOpaEvaluationEngineOutputNode(singlePolicyEvaluationResponse);
    ArrayNode allowListViolationsNode = (ArrayNode) opaEvaluationEngineOutputNode.get(OPA_OUTPUT_JSON_KEY_3);
    allowListViolationsNode.forEach(
        allowListViolation -> extractAndPopulateViolation(allowListViolations, allowListViolation));
    ArrayNode denyListViolationsNode = (ArrayNode) opaEvaluationEngineOutputNode.get(OPA_OUTPUT_JSON_KEY_4);
    denyListViolationsNode.forEach(
        denyListViolation -> extractAndPopulateViolation(denyListViolations, denyListViolation));
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

  private static void extractAndPopulateViolation(List<Violation> violations, JsonNode violationNode) {
    Violation violation = extractViolationFromJsonNode(violationNode);
    if (violation != null) {
      violations.add(violation);
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
    // TODO: Remove null parameters from the rule
    return rule;
  }
}
