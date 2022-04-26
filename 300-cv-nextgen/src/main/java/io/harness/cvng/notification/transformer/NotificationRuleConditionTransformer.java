/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.notification.transformer;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.notification.beans.NotificationRuleCondition;
import io.harness.cvng.notification.beans.NotificationRuleDTO;
import io.harness.cvng.notification.entities.NotificationRule;

import java.util.List;

public abstract class NotificationRuleConditionTransformer<E extends NotificationRule, S
                                                               extends NotificationRuleCondition> {
  public abstract E getEntity(ProjectParams projectParams, NotificationRuleDTO notificationRuleDTO);

  public final NotificationRuleDTO getDto(E notificationRule) {
    return NotificationRuleDTO.builder()
        .orgIdentifier(notificationRule.getOrgIdentifier())
        .projectIdentifier(notificationRule.getProjectIdentifier())
        .identifier(notificationRule.getIdentifier())
        .name(notificationRule.getName())
        .type(notificationRule.getType())
        .notificationMethod(notificationRule.getNotificationMethod())
        .conditions((List<NotificationRuleCondition>) getSpec(notificationRule))
        .enabled(notificationRule.isEnabled())
        .build();
  }

  protected abstract List<S> getSpec(E notificationRule);
}
