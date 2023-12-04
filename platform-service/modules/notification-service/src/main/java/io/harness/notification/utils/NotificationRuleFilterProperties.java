/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.notification.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.notification.entities.NotificationEntity;
import io.harness.notification.entities.NotificationEvent;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PL)
public class NotificationRuleFilterProperties {
  String searchTerm;
  NotificationEntity notificationEntity;
  NotificationEvent notificationEvent;
}
