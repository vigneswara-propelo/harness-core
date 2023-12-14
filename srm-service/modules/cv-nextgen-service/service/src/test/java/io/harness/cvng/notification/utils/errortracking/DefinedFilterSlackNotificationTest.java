/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.cvng.notification.utils.errortracking;

import static io.harness.cvng.notification.utils.NotificationRuleConstants.MONITORED_SERVICE_NAME;
import static io.harness.cvng.notification.utils.NotificationRuleConstants.MONITORED_SERVICE_URL;
import static io.harness.cvng.notification.utils.errortracking.interfaces.AggregatedNotificationTest.TEST_BASE_URL;
import static io.harness.cvng.notification.utils.errortracking.interfaces.AggregatedNotificationTest.TEST_ENVIRONMENT_ID;
import static io.harness.cvng.notification.utils.errortracking.interfaces.AggregatedNotificationTest.TEST_TIME_MILLIS;
import static io.harness.cvng.notification.utils.errortracking.interfaces.ErrorTrackingNotification.ENVIRONMENT_NAME;
import static io.harness.cvng.notification.utils.errortracking.interfaces.ErrorTrackingNotification.EVENT_STATUS;
import static io.harness.cvng.notification.utils.errortracking.interfaces.ErrorTrackingNotification.NOTIFICATION_EVENT_TRIGGER_LIST;
import static io.harness.cvng.notification.utils.errortracking.interfaces.SlackNotification.NOTIFICATION_NAME;
import static io.harness.cvng.notification.utils.errortracking.interfaces.SlackNotification.NOTIFICATION_URL;
import static io.harness.cvng.notification.utils.errortracking.interfaces.SlackNotification.SLACK_EVENT_DETAILS_BUTTON;
import static io.harness.cvng.notification.utils.errortracking.interfaces.SlackNotification.SLACK_FORMATTED_VERSION_LIST;
import static io.harness.rule.OwnerRule.JAMES_RICKS;

import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.errortracking.ErrorTrackingNotificationData;
import io.harness.cvng.beans.errortracking.Scorecard;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.notification.beans.ErrorTrackingEventStatus;
import io.harness.cvng.notification.beans.ErrorTrackingEventType;
import io.harness.cvng.notification.entities.MonitoredServiceNotificationRule;
import io.harness.cvng.notification.entities.MonitoredServiceNotificationRule.MonitoredServiceCodeErrorCondition;
import io.harness.cvng.notification.entities.MonitoredServiceNotificationRule.MonitoredServiceNotificationRuleCondition;
import io.harness.cvng.notification.entities.NotificationRule;
import io.harness.rule.Owner;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DefinedFilterSlackNotificationTest {
  ErrorTrackingNotificationData errorTrackingNotificationData;
  Long savedFilterId;
  Timestamp from;
  Timestamp to;

  boolean aggregated = true;

  List<ErrorTrackingEventStatus> errorTrackingEventStatus;
  List<ErrorTrackingEventType> errorTrackingEventTypes;

  MonitoredServiceCodeErrorCondition codeErrorCondition;
  MonitoredService monitoredService;
  NotificationRule notificationRule;

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

    List<Scorecard> scorecards = new ArrayList<>();
    Scorecard scorecard = Scorecard.builder()
                              .newHitCount(1)
                              .hitCount(10)
                              .versionIdentifier("testVersion")
                              .accountIdentifier(monitoredService.getAccountId())
                              .organizationIdentifier(monitoredService.getOrgIdentifier())
                              .projectIdentifier(monitoredService.getProjectIdentifier())
                              .serviceIdentifier(monitoredService.getServiceIdentifier())
                              .environmentIdentifier(TEST_ENVIRONMENT_ID)
                              .build();
    scorecards.add(scorecard);

    from = new Timestamp(TEST_TIME_MILLIS);
    to = new Timestamp(TEST_TIME_MILLIS + 6000);

    errorTrackingNotificationData =
        ErrorTrackingNotificationData.builder().scorecards(scorecards).from(from).to(to).build();

    errorTrackingEventStatus = List.of(ErrorTrackingEventStatus.values());

    errorTrackingEventTypes = Arrays.stream(ErrorTrackingEventType.values())
                                  .filter(type -> ErrorTrackingEventType.TIMER != type)
                                  .collect(Collectors.toList());

    codeErrorCondition = MonitoredServiceCodeErrorCondition.builder()
                             .aggregated(aggregated)
                             .errorTrackingEventStatus(errorTrackingEventStatus)
                             .errorTrackingEventTypes(errorTrackingEventTypes)
                             .savedFilterId(savedFilterId)
                             .build();

    List<MonitoredServiceNotificationRuleCondition> codeErrorConditions = Collections.singletonList(codeErrorCondition);

    notificationRule =
        MonitoredServiceNotificationRule.builder().conditions(codeErrorConditions).name("testNotificationRule").build();
  }

  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void getNotificationDataMapTest() {
    final Map<String, String> notificationDataMap =
        DefinedFilterSlackNotification.getNotificationDataMap(errorTrackingNotificationData, codeErrorCondition,
            TEST_BASE_URL, monitoredService, notificationRule, TEST_ENVIRONMENT_ID);
    assert notificationDataMap.get(ENVIRONMENT_NAME).equals(TEST_ENVIRONMENT_ID);
    assert notificationDataMap.get(EVENT_STATUS).equals("New Events, Critical Events, Resurfaced Events");
    assert notificationDataMap.get(NOTIFICATION_EVENT_TRIGGER_LIST)
        .equals(
            "Exceptions, Log Errors, Http Errors, Custom Errors, Swallowed Exceptions, Caught Exceptions, Uncaught Exceptions, Log Warnings");
    assert notificationDataMap.get(MONITORED_SERVICE_URL)
        .equals(
            "https://testurl.com/account/testAccountId/cet/orgs/testOrg/projects/testProject/etmonitoredservices/edit/testService_testEnvironment");
    assert notificationDataMap.get(MONITORED_SERVICE_NAME).equals("testService_testEnvironment");
    assert notificationDataMap.get(NOTIFICATION_URL)
        .equals(
            "https://testurl.com/account/testAccountId/cet/orgs/testOrg/projects/testProject/etmonitoredservices/edit/testService_testEnvironment");
    assert notificationDataMap.get(NOTIFICATION_NAME).equals("testNotificationRule");
    assert notificationDataMap.get(SLACK_FORMATTED_VERSION_LIST)
        .equals("Events appeared on the deployment version *testVersion*\n"
            + "<https://testurl.com/account/testAccountId/cet/orgs/testOrg/projects/testProject/eventsummary/events?env=testEnvironmentId&service=testService&dep=testVersion&fromTimestamp="
            + from.getTime() / 1000 + "&toTimestamp=" + to.getTime() / 1000
            + "&eventType=CAUGHT_EXCEPTION,CUSTOM_ERROR,HTTP_ERROR,LOGGED_ERROR,LOGGED_WARNING,SWALLOWED_EXCEPTION,UNCAUGHT_EXCEPTION&eventStatus=NewEvents|New Events (1)>   |   Critical Events (0)   |   Resurfaced Events (0)");
    assert notificationDataMap.get(SLACK_EVENT_DETAILS_BUTTON).equals("");
  }
}
