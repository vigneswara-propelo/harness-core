/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.cvng.notification.utils.errortracking.interfaces;

import static io.harness.rule.OwnerRule.JAMES_RICKS;

import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.errortracking.ErrorTrackingNotificationData;
import io.harness.cvng.beans.errortracking.Scorecard;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.notification.beans.ErrorTrackingEventStatus;
import io.harness.cvng.notification.beans.ErrorTrackingEventType;
import io.harness.cvng.notification.entities.MonitoredServiceNotificationRule;
import io.harness.rule.Owner;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AggregatedNotificationTest {
  public static final String TEST_BASE_URL = "https://testurl.com";
  public static String TEST_ENVIRONMENT_ID = "testEnvironmentId";
  public static Long TEST_TIME_MILLIS = 1700258695529L;

  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void getSlackFormattedVersionListTest() {
    final String slackFormattedVersionList = SlackNotification.getSlackFormattedVersionList(
        getTestCodeErrorCondition(), getTestNotificationData(), TEST_BASE_URL);
    assert slackFormattedVersionList.equals("Events appeared on the deployment version *testVersion*\n"
        + "<https://testurl.com/account/testAccountId/cet/orgs/testOrg/projects/testProject/eventsummary/events?env=testEnvironmentId&service=testService&dep=testVersion&fromTimestamp=1700258695&toTimestamp=1700258701&eventStatus=NewEvents|New Events (1)>   |   Critical Events (0)   |   Resurfaced Events (0)");
  }

  public static ErrorTrackingNotificationData getTestNotificationData() {
    List<String> environmentIdentifierList = Collections.singletonList(TEST_ENVIRONMENT_ID);

    MonitoredService monitoredService = MonitoredService.builder()
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

    Timestamp from = new Timestamp(TEST_TIME_MILLIS);
    Timestamp to = new Timestamp(TEST_TIME_MILLIS + 6000);

    return ErrorTrackingNotificationData.builder().scorecards(scorecards).from(from).to(to).build();
  }

  public static MonitoredServiceNotificationRule.MonitoredServiceCodeErrorCondition getTestCodeErrorCondition() {
    List<ErrorTrackingEventStatus> errorTrackingEventStatus = List.of(ErrorTrackingEventStatus.values());
    List<ErrorTrackingEventType> errorTrackingEventTypes = Arrays.stream(ErrorTrackingEventType.values())
                                                               .filter(type -> ErrorTrackingEventType.TIMER != type)
                                                               .collect(Collectors.toList());

    return MonitoredServiceNotificationRule.MonitoredServiceCodeErrorCondition.builder()
        .aggregated(true)
        .errorTrackingEventStatus(errorTrackingEventStatus)
        .errorTrackingEventTypes(errorTrackingEventTypes)
        .build();
  }
}
