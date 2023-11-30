/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.cvng.notification.utils.errortracking;

import static io.harness.cvng.core.services.impl.monitoredService.ErrorTrackingNotificationServiceImplTest.createHitSummary;
import static io.harness.cvng.notification.entities.MonitoredServiceNotificationRule.MonitoredServiceCodeErrorCondition;
import static io.harness.cvng.notification.utils.NotificationRuleConstants.MONITORED_SERVICE_NAME;
import static io.harness.cvng.notification.utils.NotificationRuleConstants.MONITORED_SERVICE_URL;
import static io.harness.cvng.notification.utils.errortracking.SavedFilterSlackNotification.SLACK_SAVED_SEARCH_FILTER_SECTION;
import static io.harness.cvng.notification.utils.errortracking.interfaces.AggregatedNotificationTest.TEST_BASE_URL;
import static io.harness.cvng.notification.utils.errortracking.interfaces.AggregatedNotificationTest.TEST_ENVIRONMENT_ID;
import static io.harness.cvng.notification.utils.errortracking.interfaces.ErrorTrackingNotification.ENVIRONMENT_NAME;
import static io.harness.cvng.notification.utils.errortracking.interfaces.ErrorTrackingNotification.EVENT_STATUS;
import static io.harness.cvng.notification.utils.errortracking.interfaces.SlackNotification.NOTIFICATION_URL;
import static io.harness.cvng.notification.utils.errortracking.interfaces.SlackNotification.SLACK_EVENT_DETAILS_BUTTON;
import static io.harness.cvng.notification.utils.errortracking.interfaces.SlackNotification.SLACK_FORMATTED_VERSION_LIST;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ImmediateSlackNotificationTest {
  boolean aggregated = false;

  List<ErrorTrackingEventStatus> errorTrackingEventStatus;
  List<ErrorTrackingEventType> errorTrackingEventTypes;

  MonitoredServiceCodeErrorCondition codeErrorCondition;
  MonitoredService monitoredService;
  NotificationRule notificationRule;
  ErrorTrackingHitSummary errorTrackingHitSummary;

  @Before
  public void setup() {
    List<String> environmentIdentifierList = Collections.singletonList(TEST_ENVIRONMENT_ID);

    monitoredService = MonitoredService.builder()
                           .accountId("testAccountId")
                           .orgIdentifier("testOrg")
                           .projectIdentifier("testProject")
                           .serviceIdentifier("testService")
                           .environmentIdentifierList(environmentIdentifierList)
                           .identifier("testService_testEnvironment")
                           .build();

    errorTrackingHitSummary = createHitSummary("testVersion1");

    errorTrackingEventStatus = List.of(ErrorTrackingEventStatus.values());

    errorTrackingEventTypes = Arrays.stream(ErrorTrackingEventType.values())
                                  .filter(type -> ErrorTrackingEventType.TIMER != type)
                                  .collect(Collectors.toList());

    codeErrorCondition = MonitoredServiceCodeErrorCondition.builder()
                             .aggregated(aggregated)
                             .errorTrackingEventStatus(errorTrackingEventStatus)
                             .errorTrackingEventTypes(errorTrackingEventTypes)
                             .build();

    List<MonitoredServiceNotificationRuleCondition> codeErrorConditions = Collections.singletonList(codeErrorCondition);

    notificationRule =
        MonitoredServiceNotificationRule.builder().conditions(codeErrorConditions).name("testNotificationRule").build();
  }

  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void getNotificationDataMapTest() {
    final Map<String, String> notificationDataMap = ImmediateSlackNotification.getNotificationDataMap(
        errorTrackingHitSummary, TEST_BASE_URL, monitoredService, notificationRule, TEST_ENVIRONMENT_ID);
    assert notificationDataMap.get(ENVIRONMENT_NAME).equals(TEST_ENVIRONMENT_ID);
    assert notificationDataMap.get(EVENT_STATUS).equals("New Events");
    assert notificationDataMap.get(MONITORED_SERVICE_URL)
        .equals(
            "https://testurl.com/account/testAccountId/cet/orgs/testOrg/projects/testProject/etmonitoredservices/edit/testService_testEnvironment");
    assert notificationDataMap.get(MONITORED_SERVICE_NAME).equals("testService_testEnvironment");
    assert notificationDataMap.get(NOTIFICATION_URL)
        .equals(
            "https://testurl.com/account/testAccountId/cet/orgs/testOrg/projects/testProject/etmonitoredservices/edit/testService_testEnvironment");

    assert notificationDataMap.get(SLACK_SAVED_SEARCH_FILTER_SECTION).equals("");

    assert notificationDataMap.get(SLACK_FORMATTED_VERSION_LIST)
        .equals("Events appeared on the deployment version *testVersion1*\n"
            + "```stacktrace line 1\n"
            + "stacktrace line 2\n"
            + "stacktrace line 3```");
    assert notificationDataMap.get(SLACK_EVENT_DETAILS_BUTTON)
        .equals(
            "{\"type\": \"actions\",\"elements\": [{\"type\": \"button\",\"text\": {\"type\": \"plain_text\",\"text\": \"View Event Details\",\"emoji\": true},\"url\": \"https://testurl.com/account/testAccountId/cet/orgs/testOrg/projects/testProject/eventsummary/events/arc?request=27&environment=testEnvironmentId&harnessService=testService&dep=testVersion1&fromTimestamp=1700529307&toTimestamp=1700529367\"}]}");
  }
}