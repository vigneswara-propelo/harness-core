/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.changehandlers;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ServicesChangeDataHandlerTest extends CategoryTest {
  private final String orgIdentifier = "orgId";
  private final String projectIdentifier = "projId";
  private final String serviceIdentifier = "serviceId";

  @Test
  @Owner(developers = OwnerRule.MANISH)
  @Category(UnitTests.class)
  public void testGetFullyQualifiedIdentifierAccountService() {
    DBObject dbObject = new BasicDBObject();
    dbObject.put("orgIdentifier", null);
    dbObject.put("projectIdentifier", null);
    dbObject.put("identifier", serviceIdentifier);

    String fullyQualifiedIdentifier = ServicesChangeDataHandler.getFullyQualifiedIdentifier(dbObject);
    assertThat(fullyQualifiedIdentifier).isEqualTo("account." + serviceIdentifier);
  }

  @Test
  @Owner(developers = OwnerRule.MANISH)
  @Category(UnitTests.class)
  public void testGetFullyQualifiedIdentifierOrgService() {
    DBObject dbObject = new BasicDBObject();
    dbObject.put("orgIdentifier", orgIdentifier);
    dbObject.put("projectIdentifier", null);
    dbObject.put("identifier", serviceIdentifier);

    String fullyQualifiedIdentifier = ServicesChangeDataHandler.getFullyQualifiedIdentifier(dbObject);
    assertThat(fullyQualifiedIdentifier).isEqualTo("org." + serviceIdentifier);
  }

  @Test
  @Owner(developers = OwnerRule.MANISH)
  @Category(UnitTests.class)
  public void testGetFullyQualifiedIdentifierProjectService() {
    DBObject dbObject = new BasicDBObject();
    dbObject.put("orgIdentifier", orgIdentifier);
    dbObject.put("projectIdentifier", projectIdentifier);
    dbObject.put("identifier", serviceIdentifier);

    String fullyQualifiedIdentifier = ServicesChangeDataHandler.getFullyQualifiedIdentifier(dbObject);
    assertThat(fullyQualifiedIdentifier).isEqualTo(serviceIdentifier);
  }
}
