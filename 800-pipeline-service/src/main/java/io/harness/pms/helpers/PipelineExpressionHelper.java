/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.helpers;

import io.harness.PipelineServiceConfiguration;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntityUtils;
import io.harness.pms.pipeline.service.PMSPipelineService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;

@Singleton
public class PipelineExpressionHelper {
  @Inject PMSPipelineService pmsPipelineService;
  @Inject PipelineServiceConfiguration pipelineServiceConfiguration;

  public String generateUrl(Ambiance ambiance) {
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String orgId = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectId = AmbianceUtils.getProjectIdentifier(ambiance);
    String moduleName = "cd";
    Optional<PipelineEntity> pipelineEntity =
        pmsPipelineService.get(accountId, orgId, projectId, ambiance.getMetadata().getPipelineIdentifier(), false);

    if (!EmptyPredicate.isEmpty(ambiance.getMetadata().getModuleType())) {
      moduleName = ambiance.getMetadata().getModuleType();
    } else if (pipelineEntity.isPresent()) {
      moduleName = PipelineEntityUtils.getModuleNameFromPipelineEntity(pipelineEntity.get(), "cd");
    }
    return String.format("%s/account/%s/%s/orgs/%s/projects/%s/pipelines/%s/executions/%s/pipeline",
        pipelineServiceConfiguration.getPipelineServiceBaseUrl(), accountId, moduleName, orgId, projectId,
        ambiance.getMetadata().getPipelineIdentifier(), ambiance.getPlanExecutionId());
  }
}
