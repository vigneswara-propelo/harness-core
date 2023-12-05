/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.migration;

import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.entities.NGSchema;
import io.harness.ng.core.migration.schema.ParentUniqueIdSchema;

import java.util.ArrayList;
import java.util.List;

public class ParentUniqueIdMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return "parentuniqueidentities";
  }

  @Override
  public Class<? extends NGSchema> getSchemaClass() {
    return ParentUniqueIdSchema.class;
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return new ArrayList<Class<? extends MigrationDetails>>() {
      { add(ParentUniqueIdMigration.class); }
    };
  }
}
