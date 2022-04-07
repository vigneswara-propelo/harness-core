package io.harness.ng.core.migration;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.DbAliases.NG_MANAGER;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.beans.MigrationType;
import io.harness.migration.entities.NGSchema;

import java.util.Map;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@StoreIn(NG_MANAGER)
@Document("schema_usergroup")
@TypeAlias("schema_usergroup")
@OwnedBy(PL)
public class UserGroupMigrationSchema extends NGSchema {
  public UserGroupMigrationSchema(
      String id, Long createdAt, Long lastUpdatedAt, String name, Map<MigrationType, Integer> migrationDetails) {
    super(id, createdAt, lastUpdatedAt, name, migrationDetails);
  }
}
