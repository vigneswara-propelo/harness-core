/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.utils.RbacConstants.DELEGATE_RESOURCE_TYPE;
import static io.harness.delegate.utils.RbacConstants.DELEGATE_VIEW_PERMISSION;

import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.accesscontrol.acl.api.AccessCheckResponseDTO;
import io.harness.accesscontrol.acl.api.AccessControlDTO;
import io.harness.accesscontrol.acl.api.PermissionCheckDTO;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateGroup;
import io.harness.delegate.beans.DelegateGroup.DelegateGroupKeys;
import io.harness.ng.core.dto.EntityScopeInfo;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import dev.morphia.query.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PL)
public class DelegateRbacHelper {
  @Inject private AccessControlClient accessControlClient;
  @Inject private HPersistence persistence;

  public List<String> getViewPermittedDelegateGroupIds(
      List<String> delegateGroupIds, String accountId, String orgId, String projectId) {
    if (isEmpty(delegateGroupIds)) {
      return null;
    }
    Query<DelegateGroup> delegateGroupQuery =
        persistence.createQuery(DelegateGroup.class).field(DelegateGroupKeys.uuid).in(delegateGroupIds);
    List<String> delegateGroupIdentifiers =
        delegateGroupQuery.asList().stream().map(DelegateGroup::getIdentifier).collect(Collectors.toList());

    Map<EntityScopeInfo, List<String>> delegateGroupIdMap =
        delegateGroupIdentifiers.stream().collect(groupingBy(delegateGroupIdentifier
            -> getEntityScopeInfoFromDelegateGroupId(delegateGroupIdentifier, accountId, orgId, projectId)));

    List<PermissionCheckDTO> permissionChecks =
        delegateGroupIdentifiers.stream()
            .map(delegateGroupIdentifier
                -> PermissionCheckDTO.builder()
                       .permission(DELEGATE_VIEW_PERMISSION)
                       .resourceIdentifier(delegateGroupIdentifier)
                       .resourceScope(ResourceScope.of(accountId, orgId, projectId))
                       .resourceType(DELEGATE_RESOURCE_TYPE)
                       .build())
            .collect(Collectors.toList());
    AccessCheckResponseDTO accessCheckResponse = accessControlClient.checkForAccessOrThrow(permissionChecks);

    List<String> permittedDelegateGroupIds = new ArrayList<>();
    for (AccessControlDTO accessControlDTO : accessCheckResponse.getAccessControlList()) {
      if (accessControlDTO.isPermitted()) {
        permittedDelegateGroupIds.add(
            delegateGroupIdMap.get(getEntityScopeInfoFromAccessControlDTO(accessControlDTO)).get(0));
      }
    }
    if (isEmpty(permittedDelegateGroupIds)) {
      return permittedDelegateGroupIds;
    }
    Query<DelegateGroup> delegateGroupQueryToConvertIdentifiersToId =
        persistence.createQuery(DelegateGroup.class).field(DelegateGroupKeys.identifier).in(permittedDelegateGroupIds);
    return delegateGroupQueryToConvertIdentifiersToId.asList()
        .stream()
        .map(DelegateGroup::getUuid)
        .collect(Collectors.toList());
  }

  public List<DelegateGroup> getViewPermittedDelegateGroups(
      List<DelegateGroup> delegateGroups, String accountId, String orgId, String projectId) {
    if (isEmpty(delegateGroups)) {
      return null;
    }
    Map<EntityScopeInfo, List<DelegateGroup>> delegateGroupIdMap = delegateGroups.stream().collect(
        groupingBy(delegateGroup -> getEntityScopeInfoFromDelegateGroup(delegateGroup, accountId, orgId, projectId)));

    List<PermissionCheckDTO> permissionChecks =
        delegateGroups.stream()
            .map(delegateGroup
                -> PermissionCheckDTO.builder()
                       .permission(DELEGATE_VIEW_PERMISSION)
                       .resourceIdentifier(delegateGroup.getIdentifier())
                       .resourceScope(ResourceScope.of(accountId, orgId, projectId))
                       .resourceType(DELEGATE_RESOURCE_TYPE)
                       .build())
            .collect(Collectors.toList());
    AccessCheckResponseDTO accessCheckResponse = accessControlClient.checkForAccessOrThrow(permissionChecks);

    List<DelegateGroup> permittedDelegateGroups = new ArrayList<>();
    for (AccessControlDTO accessControlDTO : accessCheckResponse.getAccessControlList()) {
      if (accessControlDTO.isPermitted()) {
        permittedDelegateGroups.add(
            delegateGroupIdMap.get(getEntityScopeInfoFromAccessControlDTO(accessControlDTO)).get(0));
      }
    }
    return permittedDelegateGroups;
  }

  private static EntityScopeInfo getEntityScopeInfoFromDelegateGroupId(
      String delegateGroupId, String accountId, String orgId, String projectId) {
    return EntityScopeInfo.builder()
        .accountIdentifier(accountId)
        .orgIdentifier(orgId)
        .projectIdentifier(projectId)
        .identifier(delegateGroupId)
        .build();
  }

  private static EntityScopeInfo getEntityScopeInfoFromDelegateGroup(
      DelegateGroup delegateGroup, String accountId, String orgId, String projectId) {
    return EntityScopeInfo.builder()
        .accountIdentifier(accountId)
        .orgIdentifier(orgId)
        .projectIdentifier(projectId)
        .identifier(delegateGroup.getIdentifier())
        .build();
  }

  private static EntityScopeInfo getEntityScopeInfoFromAccessControlDTO(AccessControlDTO accessControlDTO) {
    return EntityScopeInfo.builder()
        .accountIdentifier(accessControlDTO.getResourceScope().getAccountIdentifier())
        .orgIdentifier(isBlank(accessControlDTO.getResourceScope().getOrgIdentifier())
                ? null
                : accessControlDTO.getResourceScope().getOrgIdentifier())
        .projectIdentifier(isBlank(accessControlDTO.getResourceScope().getProjectIdentifier())
                ? null
                : accessControlDTO.getResourceScope().getProjectIdentifier())
        .identifier(accessControlDTO.getResourceIdentifier())
        .build();
  }
}
