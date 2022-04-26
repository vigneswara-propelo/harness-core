/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.notification.transformer;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.notification.beans.NotificationRuleDTO;
import io.harness.cvng.notification.beans.NotificationRuleType;
import io.harness.cvng.notification.beans.SLONotificationRuleCondition;
import io.harness.cvng.notification.beans.SLONotificationRuleCondition.SLONotificationRuleConditionSpec;
import io.harness.cvng.notification.entities.SLONotificationRule;
import io.harness.cvng.notification.entities.SLONotificationRule.SLONotificationRuleEntityCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SLONotificationRuleConditionTransformer
    extends NotificationRuleConditionTransformer<SLONotificationRule, SLONotificationRuleCondition> {
  @Override
  public SLONotificationRule getEntity(ProjectParams projectParams, NotificationRuleDTO notificationRuleDTO) {
    List<SLONotificationRuleCondition> sloNotificationRuleConditions = new ArrayList<>();
    notificationRuleDTO.getConditions().forEach(
        condition -> sloNotificationRuleConditions.add((SLONotificationRuleCondition) condition));

    return SLONotificationRule.builder()
        .accountId(projectParams.getAccountIdentifier())
        .orgIdentifier(projectParams.getOrgIdentifier())
        .projectIdentifier(projectParams.getProjectIdentifier())
        .identifier(notificationRuleDTO.getIdentifier())
        .name(notificationRuleDTO.getName())
        .enabled(notificationRuleDTO.isEnabled())
        .type(NotificationRuleType.SLO)
        .notificationMethod(notificationRuleDTO.getNotificationMethod())
        .conditions(sloNotificationRuleConditions.stream()
                        .map(condition
                            -> SLONotificationRuleEntityCondition.builder()
                                   .conditionType(condition.getConditionType())
                                   .threshold(condition.getSpec().getThreshold())
                                   .build())
                        .collect(Collectors.toList()))
        .build();
  }

  @Override
  protected List<SLONotificationRuleCondition> getSpec(SLONotificationRule notificationRule) {
    return notificationRule.getConditions()
        .stream()
        .map(condition
            -> SLONotificationRuleCondition.builder()
                   .conditionType(condition.getConditionType())
                   .spec(SLONotificationRuleConditionSpec.builder().threshold(condition.getThreshold()).build())
                   .build())
        .collect(Collectors.toList());
  }
}
