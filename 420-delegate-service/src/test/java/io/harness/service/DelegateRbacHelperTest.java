/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.VIKAS_M;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.harness.DelegateServiceTestBase;
import io.harness.accesscontrol.acl.api.AccessCheckResponseDTO;
import io.harness.accesscontrol.acl.api.AccessControlDTO;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateGroup;
import io.harness.delegate.beans.DelegateGroupStatus;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.service.impl.DelegateRbacHelper;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PL)
public class DelegateRbacHelperTest extends DelegateServiceTestBase {
  @Mock AccessControlClient accessControlClient;
  @Mock HPersistence hPersistence;
  @InjectMocks DelegateRbacHelper delegateRbacHelper;
  private static String ACCOUNT_ID;
  private static String ORG_ID;
  private static String PROJECT_ID;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    ACCOUNT_ID = "ACCOUNT_ID";
    ORG_ID = "ORG_ID";
    PROJECT_ID = "PROJECT_ID";
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void getPermittedDelegateIds_CalledWithEmptyList_ReturnsNull() {
    assertThat(delegateRbacHelper.getViewPermittedDelegateGroupIds(emptyList(), ACCOUNT_ID, ORG_ID, PROJECT_ID))
        .isEqualTo(null);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void getPermittedDelegateGroup_CalledWithEmptyDelegateGroupList_ReturnsNull() {
    assertThat(delegateRbacHelper.getViewPermittedDelegateGroups(emptyList(), ACCOUNT_ID, ORG_ID, PROJECT_ID))
        .isEqualTo(null);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void getPermittedDelegateGroups_returnPermittedDelegateGroups() {
    DelegateGroup delegateGroup1 = DelegateGroup.builder()
                                       .accountId(ACCOUNT_ID)
                                       .owner(DelegateEntityOwner.builder().identifier("ORG_ID/PROJECT_ID").build())
                                       .status(DelegateGroupStatus.ENABLED)
                                       .name("delegateGroup1")
                                       .identifier("delegateGroup1")
                                       .ng(true)
                                       .uuid("UUD1")
                                       .build();
    DelegateGroup delegateGroup2 = DelegateGroup.builder()
                                       .accountId(ACCOUNT_ID)
                                       .owner(DelegateEntityOwner.builder().identifier("ORG_ID/PROJECT_ID").build())
                                       .status(DelegateGroupStatus.ENABLED)
                                       .name("delegateGroup2")
                                       .identifier("delegateGroup2")
                                       .ng(true)
                                       .uuid("UUID2")
                                       .build();
    List<DelegateGroup> delegateGroups = List.of(delegateGroup1, delegateGroup2);
    ResourceScope resourceScope = ResourceScope.of(ACCOUNT_ID, ORG_ID, PROJECT_ID);
    List<AccessControlDTO> accessControlDTOList = List.of(AccessControlDTO.builder()
                                                              .permitted(true)
                                                              .resourceScope(resourceScope)
                                                              .resourceIdentifier("delegateGroup1")
                                                              .build(),
        AccessControlDTO.builder()
            .resourceScope(resourceScope)
            .resourceIdentifier("delegateGroup2")
            .permitted(false)
            .build());
    AccessCheckResponseDTO accessCheckResponse =
        AccessCheckResponseDTO.builder().accessControlList(accessControlDTOList).build();
    when(accessControlClient.checkForAccessOrThrow(any())).thenReturn(accessCheckResponse);
    List<DelegateGroup> permittedDelegateGroupIds =
        delegateRbacHelper.getViewPermittedDelegateGroups(delegateGroups, ACCOUNT_ID, ORG_ID, PROJECT_ID);
    assertThat(permittedDelegateGroupIds.size()).isEqualTo(1);
    assertThat(permittedDelegateGroupIds.get(0)).isEqualTo(delegateGroup1);
  }
}
