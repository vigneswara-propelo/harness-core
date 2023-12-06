/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.migration.tasks.parentid;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.MongoConfig.NO_LIMIT;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.CloseableIterator;

@Slf4j
@OwnedBy(PL)
public class AddParentUniqueIdMigrationOrganization implements NGMigration {
  public static final int BATCH_SIZE = 500;
  public static final String ORG_PARENT_UNIQUE_ID_GENERATION_LOG_CONST =
      "[NGUpdateParentUniqueIdOrganizationMigration]:";
  private final MongoTemplate mongoTemplate;

  @Inject
  public AddParentUniqueIdMigrationOrganization(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public void migrate() {
    log.info(format(
        "%s Starting migration for Entity Type: [%s] ", ORG_PARENT_UNIQUE_ID_GENERATION_LOG_CONST, "Organization"));
    int migratedCounter = 0;
    int totalCounter = 0;
    int updateCounter = 0;
    int batchSizeCounter = 0;

    Query documentQuery = new Query(new Criteria());
    log.info(format("%s Entity Type: [%s], total count: [%s]", ORG_PARENT_UNIQUE_ID_GENERATION_LOG_CONST,
        "Organization", mongoTemplate.count(documentQuery, Organization.class)));

    BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Organization.class);

    String idValue = null;
    try (CloseableIterator<Organization> iterator =
             mongoTemplate.stream(documentQuery.limit(NO_LIMIT).maxTimeMsec(MAX_VALUE), Organization.class)) {
      while (iterator.hasNext()) {
        totalCounter++;
        Organization nextOrg = iterator.next();
        if (null != nextOrg && isNotEmpty(nextOrg.getParentId()) && isEmpty(nextOrg.getParentUniqueId())) {
          idValue = nextOrg.getId();
          updateCounter++;
          batchSizeCounter++;
          Update update = new Update().set(OrganizationKeys.parentUniqueId, nextOrg.getAccountIdentifier());
          bulkOperations.updateOne(new Query(Criteria.where("_id").is(idValue)), update);
          if (batchSizeCounter == BATCH_SIZE) {
            migratedCounter += bulkOperations.execute().getModifiedCount();
            bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Organization.class);
            batchSizeCounter = 0;
          }
        }
      }
      if (batchSizeCounter > 0) {
        migratedCounter += bulkOperations.execute().getModifiedCount();
      }
    } catch (Exception exc) {
      log.error(
          format("%s job failed for Entity Type [%s]", ORG_PARENT_UNIQUE_ID_GENERATION_LOG_CONST, "Organization"), exc);
    }
    log.info(format(
        "%s Migration for Entity Type: [%s]. Total documents: [%d], documents to Update: [%s], Successful: [%d], Failed: [%d]",
        ORG_PARENT_UNIQUE_ID_GENERATION_LOG_CONST, "Organization", totalCounter, updateCounter, migratedCounter,
        updateCounter - migratedCounter));
  }
}
