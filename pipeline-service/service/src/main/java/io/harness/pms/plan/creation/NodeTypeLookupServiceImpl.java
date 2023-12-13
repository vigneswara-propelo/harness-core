/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.plan.creation;

import static io.harness.cf.pipeline.FeatureFlagStageFilterJsonCreator.FEATURE_FLAG_SUPPORTED_TYPE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.exception.InvalidRequestException;
import io.harness.plan.Node;
import io.harness.pms.contracts.steps.SdkStep;
import io.harness.pms.pipeline.CommonStepInfo;
import io.harness.pms.sdk.PmsSdkInstanceService;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true,
    components = {HarnessModuleComponent.CDS_SERVICE_ENVIRONMENT})
public class NodeTypeLookupServiceImpl implements NodeTypeLookupService {
  @Inject private PmsSdkInstanceService pmsSdkInstanceService;

  @Inject private CommonStepInfo commonStepInfo;

  @Override
  public String findNodeTypeServiceName(String nodeType) {
    Map<String, Map<String, Set<String>>> map = pmsSdkInstanceService.getInstanceNameToSupportedTypes();
    if (isEmpty(map)) {
      throw new InvalidRequestException("Supported Types Map is empty");
    }
    for (Map.Entry<String, Map<String, Set<String>>> entry : map.entrySet()) {
      Set<String> supportedNodeTypes = new HashSet<>();
      for (Set<String> stringSet : entry.getValue().values()) {
        supportedNodeTypes.addAll(stringSet);
        if (isEmpty(supportedNodeTypes)) {
          continue;
        }

        if (supportedNodeTypes.stream().anyMatch(st -> st.equals(nodeType))) {
          if (nodeType.equals(FEATURE_FLAG_SUPPORTED_TYPE)) {
            return "cf";
          }
          if (nodeType.equals("Custom")) {
            return "pms";
          }
          return entry.getKey();
        }
      }
    }

    throw new InvalidRequestException("Unknown Node type: " + nodeType);
  }

  @Override
  public Set<String> modulesThatSupportStepTypes(List<Node> planNodeList) {
    // We get the supportedSdkSteps corresponding to all the modules
    Map<String, Set<SdkStep>> allSupportedSdkSteps = pmsSdkInstanceService.getSdkSteps();
    if (isEmpty(allSupportedSdkSteps)) {
      throw new InvalidRequestException("Supported Types Map is empty");
    }

    Map<String, Set<String>> stepsSupportedByModules = new LinkedHashMap<>();

    // Mapping all the isPalleteTrue steps to the corresponding modules
    for (Map.Entry<String, Set<SdkStep>> entry : allSupportedSdkSteps.entrySet()) {
      Set<String> isPalleteTrue = new HashSet<>();
      for (SdkStep sdkStep : entry.getValue()) {
        if (sdkStep.getIsPartOfStepPallete()) {
          isPalleteTrue.add(sdkStep.getStepType().getType());
        }
      }
      if (!isPalleteTrue.isEmpty()) {
        stepsSupportedByModules.put(entry.getKey(), isPalleteTrue);
      }
    }

    Set<String> modulesThatSupportStepTypes = new HashSet<>();

    // iterating over the planNodeList and then checking if the stepType is supported by any of the modules in the
    // stepsSupportedByModules map
    for (Node node : planNodeList) {
      for (Map.Entry<String, Set<String>> entry : stepsSupportedByModules.entrySet()) {
        if (entry.getValue().contains(node.getStepType().getType().toString())) {
          modulesThatSupportStepTypes.add(entry.getKey());
        }
      }
    }

    return modulesThatSupportStepTypes;
  }
}
