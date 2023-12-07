/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.beans;

import io.harness.ssca.enforcement.constants.ViolationType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@Slf4j
public class Violation {
  List<String> artifactUuids;
  JsonNode rule;
  @Getter(AccessLevel.NONE) String type;
  @Getter(AccessLevel.NONE) String violationDetail;

  public String getViolationDetail() {
    if (StringUtils.isBlank(violationDetail)) {
      try {
        switch (type) {
                    case "allow" -> violationDetail = getAllowViolationDetail();
                    case "deny" -> violationDetail = getDenyViolationDetail();
                    default -> violationDetail = rule.toString();
                }
            } catch (Exception ex) {
                log.error("Error while parsing rule {}", rule);
                violationDetail = rule.toString();
            }
        }
        return violationDetail;
    }

    public String getType() {
        if (type.equals("allow")) {
            return ViolationType.ALLOWLIST_VIOLATION.getViolation();
        } else if (type.equals("deny")) {
            return ViolationType.DENYLIST_VIOLATION.getViolation();
        } else {
            return ViolationType.UNKNOWN_VIOLATION.getViolation();
        }
    }

    private String getAllowViolationDetail() {
        ArrayNode arrayNode = (ArrayNode) rule;
        JsonNode node = arrayNode.get(0);
        if (node.has("name")) {
            return buildAllowMessage(arrayNode, "name");
        } else if (node.has("supplier")) {
            return buildAllowMessage(arrayNode, "supplier");
        } else if (node.has("purl")) {
            return buildAllowMessage(arrayNode, "purl");
        } else if (node.has("version")) {
            return buildAllowMessage(arrayNode, "version");
        } else if (node.has("license")) {
            return buildAllowMessage(arrayNode, "license");
        }
        return null;
    }

    private String getDenyViolationDetail() {
        StringBuilder msg = new StringBuilder();
        if (rule.has("name")) {
            buildDenyMessage(msg, "name");
        }
        if (rule.has("supplier")) {
            buildDenyMessage(msg, "supplier");
        }
        if (rule.has("license")) {
            buildDenyMessage(msg, "license");
        }
        if (rule.has("purl")) {
            buildDenyMessage(msg, "purl");
        }
        if (rule.has("version")) {
            buildDenyMessage(msg, "version");
        }
        return msg.toString();
    }

    private String buildAllowMessage(ArrayNode arrayNode, String key) {
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < arrayNode.size(); i++) {
            if (msg.isEmpty()) {
                msg.append(key).append(" should ");
            } else {
                msg.append(" or ");
            }
            msg.append(getOperatorDescription(arrayNode.get(i).get(key).get("operator").asText())).append(" ")
                    .append(arrayNode.get(i).get(key).get("value").asText());
        }
        return msg.toString();

    }

    private void buildDenyMessage(StringBuilder msg, String key) {
        if (!msg.isEmpty()) {
            msg.append(" and ");
        }
        msg.append(key).append(" should not ").append(getOperatorDescription(rule.get(key).get("operator").asText()))
                .append(" ").append(rule.get(key).get("value").asText());
    }

    private static String getOperatorDescription(String operator) {
        return switch (operator) {
            case "==" -> "be equal to";
            case "<=" -> "be less than or equal to";
            case "<" -> "be less than";
            case ">=" -> "be greater than or equal to";
            case ">" -> "be greater than";
            case "!=" -> "not be equal to";
            case "!" -> "not be";
            case "~" -> " match pattern";
            default -> operator;
        };
    }
}
