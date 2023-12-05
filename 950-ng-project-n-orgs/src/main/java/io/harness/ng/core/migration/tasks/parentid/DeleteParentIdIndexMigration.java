/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.migration.tasks.parentid;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

@Slf4j
@OwnedBy(PL)
public class DeleteParentIdIndexMigration implements NGMigration {
  private final MongoTemplate mongoTemplate;
  public static final String DELETE_INDEX_PARENT_ID = "[NGDeleteIndexParentId]:";
  @Inject
  public DeleteParentIdIndexMigration(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }
  public void deleteIndex(String indexName, Class<?> entityClass) {
    try {
      mongoTemplate.indexOps(entityClass).dropIndex(indexName);
      log.info(
          format("%s Index %s deleted for entity %s", DELETE_INDEX_PARENT_ID, indexName, entityClass.getSimpleName()));
    } catch (Exception e) {
      log.error("Deleting the index: {} in collection {} ", indexName, entityClass.getSimpleName(), e);
    }
  }
  @Override
  public void migrate() {
    deleteIndex("parentIdIdentifierIdx", Organization.class);
    deleteIndex("parentIdIdentifierIdx", Project.class);
  }
}
