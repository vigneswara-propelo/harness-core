/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.infrastructure.services.impl.mapper;

import static io.harness.rule.OwnerRule.SOURABH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGEntitiesTestBase;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.ng.core.infrastructure.mappers.InfrastructureFilterHelper;
import io.harness.rule.Owner;

import junitparams.JUnitParamsRunner;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.CDC)
@RunWith(JUnitParamsRunner.class)
public class InfrastructureFilterHelperTest extends CDNGEntitiesTestBase {
  private static final String ACCOUNT_ID = "ACCOUNT_ID";
  private static final String ORG_ID = "ORG_ID";
  private static final String PROJECT_ID = "PROJECT_ID";
  private static final String ENV_ID = "ENV_ID";

  @Before
  public void setUp() throws Exception {
    openMocks(this);
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testCreateListCriteriaWithIncludeAllInfra() {
    Criteria criteria = InfrastructureFilterHelper.createListCriteria(
        ACCOUNT_ID, ORG_ID, PROJECT_ID, ENV_ID, null, null, ServiceDefinitionType.KUBERNETES, null, true);
    assertThat(StringUtils.countMatches(criteria.getCriteriaObject().get("$or").toString(), "accountId")).isEqualTo(3);
    assertThat(StringUtils.countMatches(criteria.getCriteriaObject().get("$or").toString(), "orgIdentifier"))
        .isEqualTo(3);
    assertThat(StringUtils.countMatches(criteria.getCriteriaObject().get("$or").toString(), "projectId")).isEqualTo(3);
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testCreateListCriteriaWithoutIncludeAllInfraForAccountEnv() {
    Criteria criteria = InfrastructureFilterHelper.createListCriteria(
        ACCOUNT_ID, ORG_ID, PROJECT_ID, "account." + ENV_ID, null, null, ServiceDefinitionType.KUBERNETES, null, false);
    assertThat(criteria.getCriteriaObject().get("accountId")).isEqualTo(ACCOUNT_ID);
    assertThat(criteria.getCriteriaObject().get("orgIdentifier")).isEqualTo(null);
    assertThat(criteria.getCriteriaObject().get("projectIdentifier")).isEqualTo(null);
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testCreateListCriteriaWithoutIncludeAllInfraForProjectEnv() {
    Criteria criteria = InfrastructureFilterHelper.createListCriteria(
        ACCOUNT_ID, ORG_ID, PROJECT_ID, ENV_ID, null, null, ServiceDefinitionType.KUBERNETES, null, false);
    assertThat(criteria.getCriteriaObject().get("accountId")).isEqualTo(ACCOUNT_ID);
    assertThat(criteria.getCriteriaObject().get("orgIdentifier")).isEqualTo(ORG_ID);
    assertThat(criteria.getCriteriaObject().get("projectIdentifier")).isEqualTo(PROJECT_ID);
  }

  @Test
  @Owner(developers = SOURABH)
  @Category(UnitTests.class)
  public void testCreateListCriteriaWithoutIncludeAllInfraForOrgEnv() {
    Criteria criteria = InfrastructureFilterHelper.createListCriteria(
        ACCOUNT_ID, ORG_ID, PROJECT_ID, "org." + ENV_ID, null, null, ServiceDefinitionType.KUBERNETES, null, false);
    assertThat(criteria.getCriteriaObject().get("accountId")).isEqualTo(ACCOUNT_ID);
    assertThat(criteria.getCriteriaObject().get("orgIdentifier")).isEqualTo(ORG_ID);
    assertThat(criteria.getCriteriaObject().get("projectIdentifier")).isEqualTo(null);
  }
}
