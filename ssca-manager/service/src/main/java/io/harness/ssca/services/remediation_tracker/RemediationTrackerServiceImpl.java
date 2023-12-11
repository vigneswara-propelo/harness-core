/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.ssca.services.remediation_tracker;

import io.harness.exception.InvalidArgumentsException;
import io.harness.repositories.RemediationTrackerRepository;
import io.harness.spec.server.ssca.v1.model.RemediationCondition;
import io.harness.spec.server.ssca.v1.model.RemediationTrackerCreateRequestBody;
import io.harness.ssca.enforcement.executors.mongo.filter.denylist.fields.VersionField;
import io.harness.ssca.entities.remediation_tracker.RemediationStatus;
import io.harness.ssca.entities.remediation_tracker.RemediationTrackerEntity;
import io.harness.ssca.mapper.RemediationTrackerMapper;
import io.harness.ssca.services.ArtifactService;
import io.harness.ssca.services.CdInstanceSummaryService;

import com.google.inject.Inject;
import java.util.List;

public class RemediationTrackerServiceImpl implements RemediationTrackerService {
  @Inject RemediationTrackerRepository repository;

  @Inject ArtifactService artifactService;

  @Inject CdInstanceSummaryService cdInstanceSummaryService;
  @Override
  public String createRemediationTracker(
      String accountId, String orgId, String projectId, RemediationTrackerCreateRequestBody body) {
    validateRemediationCreateRequest(body);
    RemediationTrackerEntity remediationTracker =
        RemediationTrackerEntity.builder()
            .accountIdentifier(accountId)
            .orgIdentifier(orgId)
            .projectIdentifier(projectId)
            .contactInfo(RemediationTrackerMapper.getContactInfo(body.getContact()))
            .condition(RemediationTrackerMapper.getRemediationCondition(body.getRemediationCondition()))
            .vulnerabilityInfo(RemediationTrackerMapper.getVulnerabilityInfo(body.getVulnerabilityInfo()))
            .status(RemediationStatus.ON_GOING)
            .build();
    remediationTracker = repository.save(remediationTracker);
    return remediationTracker.getUuid();
  }

  private void validateRemediationCreateRequest(RemediationTrackerCreateRequestBody body) {
    if (body.getRemediationCondition().getOperator() != RemediationCondition.OperatorEnum.ALL
        && body.getRemediationCondition().getOperator() != RemediationCondition.OperatorEnum.MATCHES) {
      List<Integer> versions = VersionField.getVersion(body.getVulnerabilityInfo().getComponentVersion());
      if (versions.size() != 3 || versions.get(0) == -1) {
        throw new InvalidArgumentsException(
            "Unsupported Version Format. Semantic Versioning is required for LessThan and LessThanEquals operator.");
      }
    }
  }
}
