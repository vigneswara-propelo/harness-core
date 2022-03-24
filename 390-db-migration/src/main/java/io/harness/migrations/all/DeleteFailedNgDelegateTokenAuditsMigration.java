package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.events.DelegateNgTokenCreateEvent;
import io.harness.delegate.events.DelegateNgTokenRevokeEvent;
import io.harness.migrations.Migration;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.OutboxEvent.OutboxEventKeys;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DeleteFailedNgDelegateTokenAuditsMigration implements Migration {
  private final MongoTemplate mongoTemplate;

  @Inject
  public DeleteFailedNgDelegateTokenAuditsMigration(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  // we are deleting only those audit events that are logged before 1 March
  private static final long MARCH_1ST_2022_IN_MS = 1646092800000L;
  private static final int BATCH_SIZE = 500;

  private static final List<String> EventTypesToDelete = Arrays.asList(
      DelegateNgTokenCreateEvent.DELEGATE_TOKEN_CREATE_EVENT, DelegateNgTokenRevokeEvent.DELEGATE_TOKEN_REVOKE_EVENT);

  @Override
  public void migrate() {
    log.info("Starting the migration for deleting failed ng delegate token audit events.");

    long deleted = 0;
    try {
      DeleteResult deleteResult = runQueryWithBatch(getAllDelegateTokenEventsCriteria(), BATCH_SIZE);
      deleted = deleteResult.getDeletedCount();
    } catch (Exception e) {
      log.error("Error occurred during migration for deleting all failed ng delegate token audit events.", e);
    }
    log.info(
        "Migration complete for deleting all failed ng delegate token audit events. Deleted " + deleted + " records.");
  }

  private Criteria getAllDelegateTokenEventsCriteria() {
    Criteria criteria = new Criteria();
    criteria.and(OutboxEventKeys.eventType).in(EventTypesToDelete);
    criteria.and(OutboxEventKeys.createdAt).lt(MARCH_1ST_2022_IN_MS);
    return criteria;
  }

  private DeleteResult runQueryWithBatch(Criteria criteria, int batchSize) {
    Query query = new Query(criteria);
    query.cursorBatchSize(batchSize);
    return mongoTemplate.remove(query, OutboxEvent.class);
  }
}
