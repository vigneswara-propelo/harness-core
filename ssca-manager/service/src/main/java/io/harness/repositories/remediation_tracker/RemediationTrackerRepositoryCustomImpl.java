/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.remediation_tracker;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ssca.entities.remediation_tracker.RemediationTrackerEntity;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.SSCA)
@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
public class RemediationTrackerRepositoryCustomImpl implements RemediationTrackerRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public RemediationTrackerEntity update(Query query, Update update) {
    return mongoTemplate.findAndModify(
        query, update, new FindAndModifyOptions().returnNew(true), RemediationTrackerEntity.class);
  }
}
