/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.cvng.notification.utils.errortracking.interfaces;

import static io.harness.cvng.core.services.impl.monitoredService.ErrorTrackingNotificationServiceImplTest.createHitSummary;
import static io.harness.cvng.notification.utils.errortracking.interfaces.AggregatedNotificationTest.TEST_BASE_URL;
import static io.harness.cvng.notification.utils.errortracking.interfaces.AggregatedNotificationTest.TEST_ENVIRONMENT_ID;
import static io.harness.cvng.notification.utils.errortracking.interfaces.ImmediateNotification.buildArcScreenUrlWithParameters;
import static io.harness.cvng.notification.utils.errortracking.interfaces.ImmediateNotification.getNotificationEventTriggerList;
import static io.harness.rule.OwnerRule.JAMES_RICKS;

import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.errortracking.ErrorTrackingHitSummary;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.notification.beans.ErrorTrackingEventStatus;
import io.harness.cvng.notification.beans.ErrorTrackingEventType;
import io.harness.cvng.notification.entities.MonitoredServiceNotificationRule;
import io.harness.cvng.notification.entities.MonitoredServiceNotificationRule.MonitoredServiceNotificationRuleCondition;
import io.harness.cvng.notification.entities.NotificationRule;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ImmediateNotificationTest {
  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void getNotificationDataMapTest() {
    List<String> environmentIdentifierList = Collections.singletonList(TEST_ENVIRONMENT_ID);

    final MonitoredService monitoredService = MonitoredService.builder()
                                                  .accountId("testAccountId")
                                                  .orgIdentifier("testOrg")
                                                  .projectIdentifier("testProject")
                                                  .serviceIdentifier("testService")
                                                  .environmentIdentifierList(environmentIdentifierList)
                                                  .identifier("testService_testEnvironment")
                                                  .build();

    final ErrorTrackingHitSummary hitSummary = createHitSummary("testVersion1");

    final List<ErrorTrackingEventStatus> eventStatus = List.of(ErrorTrackingEventStatus.values());

    final List<ErrorTrackingEventType> errorTrackingEventTypes =
        Arrays.stream(ErrorTrackingEventType.values())
            .filter(type -> ErrorTrackingEventType.TIMER != type)
            .collect(Collectors.toList());

    final MonitoredServiceNotificationRule.MonitoredServiceCodeErrorCondition codeErrorCondition =
        MonitoredServiceNotificationRule.MonitoredServiceCodeErrorCondition.builder()
            .aggregated(false)
            .errorTrackingEventStatus(eventStatus)
            .errorTrackingEventTypes(errorTrackingEventTypes)
            .build();

    List<MonitoredServiceNotificationRuleCondition> codeErrorConditions = Collections.singletonList(codeErrorCondition);

    NotificationRule.CVNGSlackChannel slackChannel = new NotificationRule.CVNGSlackChannel(null, null);
    final MonitoredServiceNotificationRule notificationRule = MonitoredServiceNotificationRule.builder()
                                                                  .conditions(codeErrorConditions)
                                                                  .notificationMethod(slackChannel)
                                                                  .name("testNotificationRule")
                                                                  .build();

    final Map<String, String> notificationDataMap = ImmediateNotification.getNotificationDataMap(
        hitSummary, TEST_BASE_URL, monitoredService, notificationRule, TEST_ENVIRONMENT_ID);
    assert notificationDataMap.size() > 0;
  }

  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void getNotificationEventTriggerListTest() {
    final String notificationEventTriggerList = getNotificationEventTriggerList();
    assert notificationEventTriggerList.equals(
        "Caught Exceptions, Uncaught Exceptions, Swallowed Exceptions, Logged Errors, Logged Warnings, Http Errors, Custom Errors");
  }

  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void buildArcScreenUrlWithParametersTest() {
    final ErrorTrackingHitSummary hitSummary = createHitSummary("testVersion1");
    final String url = buildArcScreenUrlWithParameters(hitSummary, TEST_BASE_URL, "testAccount", "testOrg",
        "testProject", 88, TEST_ENVIRONMENT_ID, "testService", "testDeployment");
    assert url.equals(
        "https://testurl.com/account/testAccount/cet/orgs/testOrg/projects/testProject/eventsummary/events/arc?request=88&environment=testEnvironmentId&harnessService=testService&dep=testDeployment&fromTimestamp=1700529307&toTimestamp=1700529367");
  }
}
