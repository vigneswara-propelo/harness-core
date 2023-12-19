/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.principals.usergroups;

import static io.harness.aggregator.ACLEventProcessingConstants.UPDATE_ACTION;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.Objects.nonNull;
import static java.util.Optional.of;

import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.accesscontrol.scopes.harness.ScopeMapper;
import io.harness.aggregator.consumers.AccessControlChangeConsumer;
import io.harness.aggregator.models.UserGroupUpdateEventData;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.UserGroupDTO;
import io.harness.ng.core.dto.UserGroupFilterDTO;
import io.harness.remote.client.NGRestUtils;
import io.harness.usergroups.UserGroupClient;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Singleton
@ValidateOnExecution
@Slf4j
public class HarnessUserGroupServiceImpl implements HarnessUserGroupService {
  private final UserGroupClient userGroupClient;
  private final UserGroupService userGroupService;
  private final AccessControlChangeConsumer<UserGroupUpdateEventData> accessControlChangeConsumer;
  private final boolean enableParallelProcessingOfUserGroupUpdates;

  @Inject
  public HarnessUserGroupServiceImpl(@Named("PRIVILEGED") UserGroupClient userGroupClient,
      UserGroupService userGroupService,
      AccessControlChangeConsumer<UserGroupUpdateEventData> accessControlChangeConsumer,
      @Named("enableParallelProcessingOfUserGroupUpdates") boolean enableParallelProcessingOfUserGroupUpdates) {
    this.userGroupClient = userGroupClient;
    this.userGroupService = userGroupService;
    this.accessControlChangeConsumer = accessControlChangeConsumer;
    this.enableParallelProcessingOfUserGroupUpdates = enableParallelProcessingOfUserGroupUpdates;
  }

  @Override
  public void sync(String identifier, Scope scope) {
    HarnessScopeParams scopeParams = ScopeMapper.toParams(scope);

    try {
      UserGroupFilterDTO userGroupFilterDTO = UserGroupFilterDTO.builder()
                                                  .accountIdentifier(scopeParams.getAccountIdentifier())
                                                  .orgIdentifier(scopeParams.getOrgIdentifier())
                                                  .projectIdentifier(scopeParams.getProjectIdentifier())
                                                  .identifierFilter(Set.of(identifier))
                                                  .build();
      PageResponse<UserGroupDTO> userGroupDTOPageResponse = NGRestUtils.getResponse(
          userGroupClient.getUserGroups(scopeParams.getAccountIdentifier(), userGroupFilterDTO, 0, 1, null),
          "Could not find the user group with the given identifier");
      if (nonNull(userGroupDTOPageResponse) && userGroupDTOPageResponse.getContent().size() > 0) {
        List<UserGroupDTO> userGroupDTOList = userGroupDTOPageResponse.getContent();
        UserGroup userGroup = UserGroupFactory.buildUserGroup(userGroupDTOList.get(0));
        if (enableParallelProcessingOfUserGroupUpdates) {
          Optional<UserGroup> userGroupOptional =
              userGroupService.get(userGroup.getIdentifier(), userGroup.getScopeIdentifier());
          if (userGroupOptional.isPresent()) {
            UserGroup existingUserGroup = userGroupOptional.get();
            if (!userGroup.equals(existingUserGroup)) {
              Set<String> usersAddedToUserGroup =
                  Sets.difference(isEmpty(userGroup.getUsers()) ? Collections.emptySet() : userGroup.getUsers(),
                      isEmpty(existingUserGroup.getUsers()) ? Collections.emptySet() : existingUserGroup.getUsers());
              Set<String> usersRemovedFromUserGroup = Sets.difference(
                  isEmpty(existingUserGroup.getUsers()) ? Collections.emptySet() : existingUserGroup.getUsers(),
                  isEmpty(userGroup.getUsers()) ? Collections.emptySet() : userGroup.getUsers());
              UserGroupUpdateEventData userGroupUpdateEventData = UserGroupUpdateEventData.builder()
                                                                      .scope(of(scope))
                                                                      .usersAdded(usersAddedToUserGroup)
                                                                      .usersRemoved(usersRemovedFromUserGroup)
                                                                      .updatedUserGroup(userGroup)
                                                                      .build();
              accessControlChangeConsumer.consumeEvent(UPDATE_ACTION, null, userGroupUpdateEventData);
              userGroupService.upsert(userGroup);
            } else {
              log.debug(
                  "[HarnessUserGroupServiceImpl]: Skipping User Group update for identifier: {}, scope: {} as state is same in NGManager and ACS",
                  userGroup.getIdentifier(), userGroup.getScopeIdentifier());
            }
          } else {
            userGroupService.upsert(userGroup);
          }
        } else {
          userGroupService.upsert(userGroup);
        }
      } else {
        deleteIfPresent(identifier, scope);
      }
    } catch (Exception e) {
      log.error("Exception while syncing user groups", e);
      throw e;
    }
  }

  public void deleteIfPresent(String identifier, Scope scope) {
    log.info("Removing user group with identifier {} in scope {}.", identifier, scope.toString());
    userGroupService.deleteIfPresent(identifier, scope.toString());
  }
}
