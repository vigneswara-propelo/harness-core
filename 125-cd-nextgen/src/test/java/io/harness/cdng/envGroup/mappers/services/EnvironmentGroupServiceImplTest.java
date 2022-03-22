/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.envGroup.mappers.services;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.cdng.envGroup.services.EnvironmentGroupServiceImpl;
import io.harness.repositories.envGroup.EnvironmentGroupRepository;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

public class EnvironmentGroupServiceImplTest extends CategoryTest {
  private String ACC_ID = "accId";
  private String ORG_ID = "orgId";
  private String PRO_ID = "proId";

  @Mock private EnvironmentGroupRepository environmentGroupRepository;

  @InjectMocks private EnvironmentGroupServiceImpl environmentGroupService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGet() {
    environmentGroupService.get(ACC_ID, ORG_ID, PRO_ID, "envGroup", false);
    verify(environmentGroupRepository, times(1))
        .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
            ACC_ID, ORG_ID, PRO_ID, "envGroup", true);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testCreate() {
    environmentGroupService.create(EnvironmentGroupEntity.builder().build());
    verify(environmentGroupRepository, times(1)).create(EnvironmentGroupEntity.builder().build());
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testList() {
    Criteria criteria = new Criteria();
    Pageable pageRequest =
        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, EnvironmentGroupEntity.EnvironmentGroupKeys.createdAt));
    environmentGroupService.list(criteria, (Pageable) pageRequest, PRO_ID, ORG_ID, ACC_ID);
    verify(environmentGroupRepository, times(1)).list(criteria, pageRequest, PRO_ID, ORG_ID, ACC_ID);
  }
}
