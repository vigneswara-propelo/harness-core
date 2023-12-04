/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.notification.utils;

import static io.harness.beans.SortOrder.Builder.aSortOrder;
import static io.harness.beans.SortOrder.OrderType.DESC;

import io.harness.beans.SortOrder;
;
import io.harness.ng.beans.PageRequest;
import io.harness.notification.NotificationChannelType;
import io.harness.notification.entities.NotificationChannel.NotificationChannelKeys;
import io.harness.notification.entities.NotificationEntity;
import io.harness.notification.entities.NotificationEvent;
import io.harness.utils.PageUtils;

import java.util.List;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.data.domain.Pageable;

public class NotificationManagementApiUtils {
  public NotificationChannelFilterProperties getNotificationChannelFilterProperties(
      String searchTerm, NotificationChannelType notificationChannelType) {
    return NotificationChannelFilterProperties.builder()
        .searchTerm(searchTerm)
        .notificationChannelType(notificationChannelType)
        .build();
  }

  public NotificationRuleFilterProperties getNotificationRuleFilterProperties(
      String searchTerm, NotificationEntity notificationEntity, NotificationEvent notificationEvent) {
    return NotificationRuleFilterProperties.builder()
        .searchTerm(searchTerm)
        .notificationEvent(notificationEvent)
        .notificationEntity(notificationEntity)
        .build();
  }

  public Pageable getPageRequest(int page, int limit, String sort, String order) {
    List<SortOrder> sortOrders;
    String fieldName = getFieldName(sort);
    if (fieldName != null) {
      SortOrder.OrderType orderType = EnumUtils.getEnum(SortOrder.OrderType.class, order, DESC);
      sortOrders = List.of(aSortOrder().withField(fieldName, orderType).build());
    } else {
      sortOrders = List.of(aSortOrder().withField(NotificationChannelKeys.lastModifiedAt, DESC).build());
    }
    return PageUtils.getPageRequest(new PageRequest(page, limit, sortOrders));
  }

  private String getFieldName(String sort) {
    String fieldName;
    PageUtils.SortFields sortField = PageUtils.SortFields.fromValue(sort);
    if (sortField == null) {
      sortField = PageUtils.SortFields.UNSUPPORTED;
    }
    switch (sortField) {
      case CREATED:
        fieldName = NotificationChannelKeys.createdAt;
        break;
      case UPDATED:
        fieldName = NotificationChannelKeys.lastModifiedAt;
        break;
      case UNSUPPORTED:
      default:
        fieldName = null;
    }
    return fieldName;
  }
}
