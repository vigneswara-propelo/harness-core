/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.notification.remote.resources.v1;

import static io.harness.notification.utils.NotificationManagementResourceTypes.NOTIFICATION_MANAGEMENT;
import static io.harness.notification.utils.NotificationManagementServicePermission.DELETE_NOTIFICATION_MANAGEMENT_PERMISSION;
import static io.harness.notification.utils.NotificationManagementServicePermission.EDIT_NOTIFICATION_MANAGEMENT_PERMISSION;
import static io.harness.notification.utils.NotificationManagementServicePermission.VIEW_NOTIFICATION_MANAGEMENT_PERMISSION;

import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.notification.NotificationChannelType;
import io.harness.notification.entities.NotificationChannel;
import io.harness.notification.remote.mappers.NotificationServiceManagementMapper;
import io.harness.notification.service.api.NotificationChannelManagementService;
import io.harness.notification.utils.NotificationChannelFilterProperties;
import io.harness.notification.utils.NotificationManagementApiUtils;
import io.harness.spec.server.notification.v1.NotificationChannelsApi;
import io.harness.spec.server.notification.v1.model.NotificationChannelDTO;

import com.google.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@OwnedBy(HarnessTeam.PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class NotificationChannelApiImpl implements NotificationChannelsApi {
  private final NotificationChannelManagementService notificationChannelManagementService;
  private final NotificationServiceManagementMapper notificationServiceManagementMapper;
  private final NotificationManagementApiUtils notificationManagementApiUtils;
  private final AccessControlClient accessControlClient;

  @Override
  public Response createNotificationChannel(
      String org, String project, @Valid NotificationChannelDTO body, String harnessAccount) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(harnessAccount, org, project),
        Resource.of(NOTIFICATION_MANAGEMENT, null), EDIT_NOTIFICATION_MANAGEMENT_PERMISSION);
    NotificationChannel notificationChannel = notificationChannelManagementService.create(
        notificationServiceManagementMapper.toNotificationChannelEntity(body, harnessAccount));
    return Response.status(Response.Status.CREATED)
        .entity(notificationServiceManagementMapper.toNotificationChannelDTO(notificationChannel))
        .build();
  }

  @Override
  public Response getNotificationChannel(
      String notificationChannelIdentifier, String org, String project, String harnessAccount) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(harnessAccount, org, project),
        Resource.of(NOTIFICATION_MANAGEMENT, null), VIEW_NOTIFICATION_MANAGEMENT_PERMISSION);
    NotificationChannel notificationChannel =
        notificationChannelManagementService.get(harnessAccount, org, project, notificationChannelIdentifier);
    return Response.status(Response.Status.OK)
        .entity(notificationServiceManagementMapper.toNotificationChannelDTO(notificationChannel))
        .build();
  }

  @Override
  public Response listNotificationChannels(String org, String project, String harnessAccount, @Max(1000L) Integer limit,
      String searchTerm, String sort, String order) {
    NotificationChannelFilterProperties filterProperties =
        notificationManagementApiUtils.getNotificationChannelFilterProperties(
            searchTerm, NotificationChannelType.EMAIL);
    Pageable pageable = notificationManagementApiUtils.getPageRequest(1, limit, sort, order);
    Page<NotificationChannel> notificationChannelPage =
        notificationChannelManagementService.list(harnessAccount, org, project, pageable, filterProperties);
    Page<NotificationChannelDTO> notificationChannelResponsePage =
        notificationChannelPage.map(notificationServiceManagementMapper::toNotificationChannelDTO);
    return Response.ok().entity(notificationChannelResponsePage.getContent()).build();
  }

  @Override
  public Response updateNotificationChannel(String notificationChannelIdentifier, String org, String project,
      @Valid NotificationChannelDTO body, String harnessAccount) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(harnessAccount, org, project),
        Resource.of(NOTIFICATION_MANAGEMENT, null), VIEW_NOTIFICATION_MANAGEMENT_PERMISSION);
    NotificationChannel existingEntity =
        notificationChannelManagementService.get(harnessAccount, org, project, notificationChannelIdentifier);
    NotificationChannel entityToUpdate =
        notificationServiceManagementMapper.toNotificationChannelEntity(body, harnessAccount);
    entityToUpdate.setUuid(existingEntity.getUuid());
    NotificationChannel notificationChannel = notificationChannelManagementService.update(entityToUpdate);
    return Response.status(Response.Status.OK)
        .entity(notificationServiceManagementMapper.toNotificationChannelDTO(notificationChannel))
        .build();
  }

  @Override
  public Response deleteNotificationChannel(
      String notificationChannelIdentifier, String org, String project, String harnessAccount) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(harnessAccount, org, project),
        Resource.of(NOTIFICATION_MANAGEMENT, null), DELETE_NOTIFICATION_MANAGEMENT_PERMISSION);
    NotificationChannel notificationChannel =
        notificationChannelManagementService.get(harnessAccount, org, project, notificationChannelIdentifier);
    boolean deleted = notificationChannelManagementService.delete(notificationChannel);
    return deleted ? Response.status(Response.Status.OK).build()
                   : Response.status(Response.Status.EXPECTATION_FAILED).build();
  }
}
