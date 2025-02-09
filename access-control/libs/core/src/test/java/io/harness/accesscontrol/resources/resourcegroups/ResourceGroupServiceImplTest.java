/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.resources.resourcegroups;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.rule.OwnerRule.JIMIT_GANDHI;
import static io.harness.rule.OwnerRule.KARAN;

import static java.util.Collections.singleton;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.accesscontrol.AccessControlCoreTestBase;
import io.harness.accesscontrol.common.filter.ManagedFilter;
import io.harness.accesscontrol.resources.resourcegroups.events.ResourceGroupDeleteEvent;
import io.harness.accesscontrol.resources.resourcegroups.events.ResourceGroupUpdateEvent;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDao;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.scopes.TestScopeLevels;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.outbox.api.OutboxService;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.name.Named;
import io.serializer.HObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(PL)
public class ResourceGroupServiceImplTest extends AccessControlCoreTestBase {
  private ResourceGroupDao resourceGroupDao;
  private RoleAssignmentService roleAssignmentService;
  private ResourceGroupServiceImpl resourceGroupService;
  @Named(OUTBOX_TRANSACTION_TEMPLATE) private TransactionTemplate outboxTransactionTemplate;
  private OutboxService outboxService;

  @Before
  public void setup() {
    resourceGroupDao = mock(ResourceGroupDao.class);
    roleAssignmentService = mock(RoleAssignmentService.class);
    outboxTransactionTemplate = mock(TransactionTemplate.class);
    outboxService = mock(OutboxService.class);
    resourceGroupService = spy(new ResourceGroupServiceImpl(
        resourceGroupDao, roleAssignmentService, outboxTransactionTemplate, outboxService));
  }

  private ResourceGroup getResourceGroup(int count, boolean managed) {
    Set<String> resourceSelectors = new HashSet<>();
    for (int i = 0; i < count; i++) {
      resourceSelectors.add(randomAlphabetic(10));
    }
    return ResourceGroup.builder()
        .identifier(randomAlphabetic(10))
        .name(randomAlphabetic(10))
        .scopeIdentifier(randomAlphabetic(10))
        .allowedScopeLevels(Sets.newHashSet(TestScopeLevels.TEST_SCOPE.toString()))
        .resourceSelectors(resourceSelectors)
        .managed(managed)
        .version(17L)
        .build();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpsert() {
    ResourceGroup currentResourceGroup = getResourceGroup(5, false);
    ResourceGroup currentResourceGroupClone = (ResourceGroup) HObjectMapper.clone(currentResourceGroup);
    Set<String> newResourceSelectors = new HashSet<>();
    for (int i = 0; i < 5; i++) {
      newResourceSelectors.add(randomAlphabetic(10));
    }
    ResourceGroup resourceGroupUpdate = ResourceGroup.builder()
                                            .identifier(currentResourceGroupClone.getIdentifier())
                                            .name(currentResourceGroupClone.getName())
                                            .scopeIdentifier(currentResourceGroupClone.getScopeIdentifier())
                                            .allowedScopeLevels(currentResourceGroupClone.getAllowedScopeLevels())
                                            .resourceSelectors(newResourceSelectors)
                                            .managed(false)
                                            .build();
    ResourceGroup updatedResourceGroup = (ResourceGroup) HObjectMapper.clone(resourceGroupUpdate);
    updatedResourceGroup.setVersion(currentResourceGroup.getVersion() + 1);

    when(resourceGroupDao.get(
             resourceGroupUpdate.getIdentifier(), resourceGroupUpdate.getScopeIdentifier(), ManagedFilter.ONLY_CUSTOM))
        .thenReturn(Optional.of(currentResourceGroup));

    when(outboxTransactionTemplate.execute(any()))
        .thenAnswer(invocationOnMock
            -> invocationOnMock.getArgument(0, TransactionCallback.class)
                   .doInTransaction(new SimpleTransactionStatus()));
    when(resourceGroupDao.upsert(resourceGroupUpdate)).thenReturn(updatedResourceGroup);
    ResourceGroupUpdateEvent resourceGroupUpdateEvent = new ResourceGroupUpdateEvent(
        currentResourceGroup, updatedResourceGroup, updatedResourceGroup.getScopeIdentifier());

    when(outboxService.save(any())).thenReturn(null);

    ArgumentCaptor<ResourceGroupUpdateEvent> argumentCaptor = ArgumentCaptor.forClass(ResourceGroupUpdateEvent.class);
    ResourceGroup resourceGroupUpdateResult = resourceGroupService.upsert(resourceGroupUpdate);

    assertEquals(updatedResourceGroup, resourceGroupUpdateResult);
    verify(outboxTransactionTemplate, times(1)).execute(any());
    verify(resourceGroupDao, times(1)).get(any(), any(), any());
    verify(resourceGroupDao, times(1)).upsert(any());
    verify(outboxService, times(1)).save(argumentCaptor.capture());
    ResourceGroupUpdateEvent resourceGroupUpdateEventResult = argumentCaptor.getValue();
    assertEquals(resourceGroupUpdateEvent, resourceGroupUpdateEventResult);
  }

  @Test
  @Owner(developers = JIMIT_GANDHI)
  @Category(UnitTests.class)
  public void skipUpsert_IfStateIsSame() {
    ResourceGroup currentResourceGroup = getResourceGroup(5, false);
    ResourceGroup currentResourceGroupClone = (ResourceGroup) HObjectMapper.clone(currentResourceGroup);

    ResourceGroup updatedResourceGroup = (ResourceGroup) HObjectMapper.clone(currentResourceGroupClone);
    when(resourceGroupDao.get(updatedResourceGroup.getIdentifier(), updatedResourceGroup.getScopeIdentifier(),
             ManagedFilter.ONLY_CUSTOM))
        .thenReturn(Optional.of(currentResourceGroup));

    when(resourceGroupDao.upsert(updatedResourceGroup)).thenReturn(updatedResourceGroup);
    ResourceGroup resourceGroupUpdateResult = resourceGroupService.upsert(updatedResourceGroup);

    assertEquals(updatedResourceGroup, resourceGroupUpdateResult);
    verify(outboxTransactionTemplate, never()).execute(any());
    verify(resourceGroupDao, times(1)).get(any(), any(), any());
    verify(resourceGroupDao, never()).upsert(any());
    verify(outboxService, never()).save(any());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpsertNotFound() {
    ResourceGroup resourceGroupUpdate = getResourceGroup(5, false);
    ResourceGroup updatedResourceGroup = (ResourceGroup) HObjectMapper.clone(resourceGroupUpdate);

    when(resourceGroupDao.get(
             resourceGroupUpdate.getIdentifier(), resourceGroupUpdate.getScopeIdentifier(), ManagedFilter.ONLY_CUSTOM))
        .thenReturn(Optional.empty());

    when(resourceGroupDao.upsert(resourceGroupUpdate)).thenReturn(updatedResourceGroup);

    ResourceGroup resourceGroupUpdateResult = resourceGroupService.upsert(resourceGroupUpdate);

    assertEquals(updatedResourceGroup, resourceGroupUpdateResult);
    verify(resourceGroupDao, times(1)).get(any(), any(), any());
    verify(resourceGroupDao, times(1)).upsert(any());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpsertScopeLevelsUpdated() {
    ResourceGroup currentResourceGroup = getResourceGroup(5, true);
    ResourceGroup currentResourceGroupClone = (ResourceGroup) HObjectMapper.clone(currentResourceGroup);
    Set<String> newResourceSelectors = new HashSet<>();
    for (int i = 0; i < 5; i++) {
      newResourceSelectors.add(randomAlphabetic(10));
    }
    ResourceGroup resourceGroupUpdate = ResourceGroup.builder()
                                            .identifier(currentResourceGroupClone.getIdentifier())
                                            .name(currentResourceGroupClone.getName())
                                            .scopeIdentifier(currentResourceGroupClone.getScopeIdentifier())
                                            .allowedScopeLevels(new HashSet<>())
                                            .resourceSelectors(newResourceSelectors)
                                            .managed(true)
                                            .build();
    ResourceGroup updatedResourceGroup = (ResourceGroup) HObjectMapper.clone(resourceGroupUpdate);
    updatedResourceGroup.setVersion(currentResourceGroup.getVersion() + 1);

    when(resourceGroupDao.get(
             resourceGroupUpdate.getIdentifier(), resourceGroupUpdate.getScopeIdentifier(), ManagedFilter.ONLY_MANAGED))
        .thenReturn(Optional.of(currentResourceGroup));

    when(outboxTransactionTemplate.execute(any()))
        .thenAnswer(invocationOnMock
            -> invocationOnMock.getArgument(0, TransactionCallback.class)
                   .doInTransaction(new SimpleTransactionStatus()));
    when(resourceGroupDao.upsert(resourceGroupUpdate)).thenReturn(updatedResourceGroup);
    ResourceGroupUpdateEvent resourceGroupUpdateEvent = new ResourceGroupUpdateEvent(
        currentResourceGroup, updatedResourceGroup, updatedResourceGroup.getScopeIdentifier());

    when(outboxService.save(any())).thenReturn(null);

    ArgumentCaptor<ResourceGroupUpdateEvent> argumentCaptor = ArgumentCaptor.forClass(ResourceGroupUpdateEvent.class);
    ResourceGroup resourceGroupUpdateResult = resourceGroupService.upsert(resourceGroupUpdate);
    Set<String> removedScopeLevels =
        Sets.difference(currentResourceGroup.getAllowedScopeLevels(), updatedResourceGroup.getAllowedScopeLevels());
    when(roleAssignmentService.deleteMulti(RoleAssignmentFilter.builder()
                                               .resourceGroupFilter(singleton(currentResourceGroup.getIdentifier()))
                                               .scopeFilter("")
                                               .includeChildScopes(true)
                                               .scopeLevelFilter(removedScopeLevels)
                                               .build()))
        .thenReturn(0L);

    assertEquals(updatedResourceGroup, resourceGroupUpdateResult);
    verify(resourceGroupDao, times(1)).get(any(), any(), any());
    verify(outboxTransactionTemplate, times(1)).execute(any());
    verify(outboxService, times(1)).save(argumentCaptor.capture());
    ResourceGroupUpdateEvent resourceGroupUpdateEventResult = argumentCaptor.getValue();
    assertEquals(resourceGroupUpdateEvent, resourceGroupUpdateEventResult);
    verify(roleAssignmentService, times(1))
        .deleteMulti(RoleAssignmentFilter.builder()
                         .resourceGroupFilter(singleton(currentResourceGroup.getIdentifier()))
                         .scopeFilter("")
                         .includeChildScopes(true)
                         .scopeLevelFilter(removedScopeLevels)
                         .build());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpsertInvalid() {
    ResourceGroup currentResourceGroup = getResourceGroup(5, false);
    ResourceGroup resourceGroupUpdate = ResourceGroup.builder()
                                            .identifier(currentResourceGroup.getIdentifier())
                                            .name(currentResourceGroup.getName())
                                            .scopeIdentifier(currentResourceGroup.getScopeIdentifier())
                                            .allowedScopeLevels(new HashSet<>())
                                            .resourceSelectors(currentResourceGroup.getResourceSelectors())
                                            .managed(false)
                                            .build();
    when(resourceGroupDao.get(
             resourceGroupUpdate.getIdentifier(), resourceGroupUpdate.getScopeIdentifier(), ManagedFilter.ONLY_CUSTOM))
        .thenReturn(Optional.of(currentResourceGroup));

    resourceGroupService.upsert(resourceGroupUpdate);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testGet() {
    String identifier = randomAlphabetic(10);
    String scopeIdentifier = randomAlphabetic(10);
    List<ManagedFilter> managedFiltersList =
        Lists.newArrayList(ManagedFilter.NO_FILTER, ManagedFilter.ONLY_CUSTOM, ManagedFilter.ONLY_MANAGED);
    int invocations = 0;
    for (ManagedFilter managedFilter : managedFiltersList) {
      invocations += 1;
      when(resourceGroupDao.get(identifier, scopeIdentifier, managedFilter))
          .thenReturn(
              Optional.of(ResourceGroup.builder().scopeIdentifier(scopeIdentifier).identifier(identifier).build()));
      Optional<ResourceGroup> resourceGroup = resourceGroupService.get(identifier, scopeIdentifier, managedFilter);
      assertTrue(resourceGroup.isPresent());
      assertEquals(identifier, resourceGroup.get().getIdentifier());
      assertEquals(scopeIdentifier, resourceGroup.get().getScopeIdentifier());
      verify(resourceGroupDao, times(1)).get(identifier, scopeIdentifier, managedFilter);
      verify(resourceGroupDao, times(invocations)).get(any(), any(), any());
    }
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testList() {
    List<String> identifiers = Lists.newArrayList(randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10));
    List<ResourceGroup> resourceGroups =
        Lists.newArrayList(getResourceGroup(5, false), getResourceGroup(5, false), getResourceGroup(5, false));
    String scopeIdentifier = randomAlphabetic(10);
    List<ManagedFilter> managedFiltersList =
        Lists.newArrayList(ManagedFilter.NO_FILTER, ManagedFilter.ONLY_CUSTOM, ManagedFilter.ONLY_MANAGED);
    int invocations = 0;
    for (ManagedFilter managedFilter : managedFiltersList) {
      invocations += 1;
      when(resourceGroupDao.list(identifiers, scopeIdentifier, managedFilter)).thenReturn(resourceGroups);
      List<ResourceGroup> response = resourceGroupService.list(identifiers, scopeIdentifier, managedFilter);
      assertEquals(3, response.size());
      verify(resourceGroupDao, times(1)).list(identifiers, scopeIdentifier, managedFilter);
      verify(resourceGroupDao, times(invocations)).list(any(), any(), any());
    }
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testListScopeIdentifier() {
    PageRequest pageRequest = PageRequest.builder().pageIndex(0).build();
    List<String> scopeIdentifiers = Lists.newArrayList(randomAlphabetic(10), null);
    int invocations = 0;
    for (String scopeIdentifier : scopeIdentifiers) {
      invocations += 1;
      when(resourceGroupDao.list(pageRequest, scopeIdentifier))
          .thenReturn(PageResponse.getEmptyPageResponse(pageRequest));
      PageResponse<ResourceGroup> response = resourceGroupService.list(pageRequest, scopeIdentifier);
      assertTrue(response.isEmpty());
      verify(resourceGroupDao, times(1)).list(pageRequest, scopeIdentifier);
      verify(resourceGroupDao, times(invocations)).list(any(), any());
    }
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDeleteManagedIfPresent() {
    String identifier = randomAlphabetic(10);
    ResourceGroup resourceGroup = ResourceGroup.builder().identifier(identifier).build();
    when(resourceGroupDao.get(identifier, null, ManagedFilter.ONLY_MANAGED)).thenReturn(Optional.of(resourceGroup));
    when(outboxTransactionTemplate.execute(any()))
        .thenAnswer(invocationOnMock
            -> invocationOnMock.getArgument(0, TransactionCallback.class)
                   .doInTransaction(new SimpleTransactionStatus()));
    when(resourceGroupDao.delete(identifier, null)).thenReturn(Optional.of(resourceGroup));
    ResourceGroupDeleteEvent resourceGroupDeleteEvent =
        new ResourceGroupDeleteEvent(resourceGroup, resourceGroup.getScopeIdentifier());
    when(outboxService.save(any())).thenReturn(null);
    ArgumentCaptor<ResourceGroupDeleteEvent> argumentCaptor = ArgumentCaptor.forClass(ResourceGroupDeleteEvent.class);
    resourceGroupService.deleteManagedIfPresent(identifier);
    verify(resourceGroupDao, times(1)).get(any(), any(), any());
    verify(outboxTransactionTemplate, times(1)).execute(any());
    verify(outboxService, times(1)).save(argumentCaptor.capture());
    assertEquals(resourceGroupDeleteEvent, argumentCaptor.getValue());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDeleteManagedIfPresentNotFound() {
    String identifier = randomAlphabetic(10);
    when(resourceGroupDao.get(identifier, null, ManagedFilter.ONLY_MANAGED)).thenReturn(Optional.empty());
    resourceGroupService.deleteManagedIfPresent(identifier);
    verify(resourceGroupDao, times(1)).get(any(), any(), any());
    verify(outboxTransactionTemplate, never()).execute(any());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDeleteIfPresent() {
    String identifier = randomAlphabetic(10);
    String scopeIdentifier = randomAlphabetic(10);
    ResourceGroup resourceGroup =
        ResourceGroup.builder().scopeIdentifier(scopeIdentifier).identifier(identifier).build();
    when(resourceGroupDao.get(identifier, scopeIdentifier, ManagedFilter.ONLY_CUSTOM))
        .thenReturn(Optional.of(resourceGroup));

    when(outboxTransactionTemplate.execute(any()))
        .thenAnswer(invocationOnMock
            -> invocationOnMock.getArgument(0, TransactionCallback.class)
                   .doInTransaction(new SimpleTransactionStatus()));
    when(resourceGroupDao.delete(identifier, resourceGroup.getScopeIdentifier()))
        .thenReturn(Optional.of(resourceGroup));
    ResourceGroupDeleteEvent resourceGroupDeleteEvent =
        new ResourceGroupDeleteEvent(resourceGroup, resourceGroup.getScopeIdentifier());
    when(outboxService.save(any())).thenReturn(null);
    ArgumentCaptor<ResourceGroupDeleteEvent> argumentCaptor = ArgumentCaptor.forClass(ResourceGroupDeleteEvent.class);
    resourceGroupService.deleteIfPresent(identifier, scopeIdentifier);
    verify(resourceGroupDao, times(1)).get(any(), any(), any());
    verify(outboxTransactionTemplate, times(1)).execute(any());
    verify(outboxService, times(1)).save(argumentCaptor.capture());
    assertEquals(resourceGroupDeleteEvent, argumentCaptor.getValue());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testDeleteIfPresentNotFound() {
    String identifier = randomAlphabetic(10);
    String scopeIdentifier = randomAlphabetic(10);
    when(resourceGroupDao.get(identifier, scopeIdentifier, ManagedFilter.ONLY_CUSTOM)).thenReturn(Optional.empty());
    resourceGroupService.deleteIfPresent(identifier, scopeIdentifier);
    verify(resourceGroupDao, times(1)).get(any(), any(), any());
    verify(outboxTransactionTemplate, never()).execute(any());
  }
}
