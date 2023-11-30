/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.cvng.notification.utils.errortracking.interfaces;

import static io.harness.cvng.notification.utils.errortracking.interfaces.AggregatedNotificationTest.TEST_BASE_URL;
import static io.harness.cvng.notification.utils.errortracking.interfaces.AggregatedNotificationTest.TEST_ENVIRONMENT_ID;
import static io.harness.cvng.notification.utils.errortracking.interfaces.AggregatedNotificationTest.getTestCodeErrorCondition;
import static io.harness.cvng.notification.utils.errortracking.interfaces.AggregatedNotificationTest.getTestNotificationData;
import static io.harness.cvng.notification.utils.errortracking.interfaces.EmailNotification.getEmailMonitoredServiceNameHyperlink;
import static io.harness.cvng.notification.utils.errortracking.interfaces.EmailNotification.getEmailNotificationLink;
import static io.harness.rule.OwnerRule.JAMES_RICKS;

import io.harness.category.element.UnitTests;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.notification.entities.MonitoredServiceNotificationRule;
import io.harness.cvng.notification.entities.MonitoredServiceNotificationRule.MonitoredServiceCodeErrorCondition;
import io.harness.cvng.notification.entities.MonitoredServiceNotificationRule.MonitoredServiceNotificationRuleCondition;
import io.harness.rule.Owner;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EmailNotificationTest {
  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void getEmailFormattedVersionListTest() {
    final String emailFormattedVersionList = EmailNotification.getEmailFormattedVersionList(
        getTestCodeErrorCondition(), getTestNotificationData(), TEST_BASE_URL);
    assert emailFormattedVersionList.equals(
        "<div style=\"margin-bottom: 16px\"><span>Events appeared on the deployment version <span style=\"font-weight: bold;\">testVersion</span></span><div style =\"margin-top: 4px;\"><span><a style=\"text-decoration: none; color: #0278D5;\" href=\"https://testurl.com/account/testAccountId/cet/orgs/testOrg/projects/testProject/eventsummary/events?env=testEnvironmentId&service=testService&dep=testVersion&fromTimestamp=1700258695&toTimestamp=1700258701&eventStatus=NewEvents\">New Events (1)</a><div style=\"display: inline; border-right: 1px solid #b0b1c3; height: 20px; margin: 0px 16px 0px 16px\"></div>Critical Events (0)<div style=\"display: inline; border-right: 1px solid #b0b1c3; height: 20px; margin: 0px 16px 0px 16px\"></div>Resurfaced Events (0)</span></div></div>");
  }

  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void getEmailMonitoredServiceNameHyperlinkTest() {
    List<String> environmentIdentifierList = Collections.singletonList(TEST_ENVIRONMENT_ID);

    MonitoredService monitoredService = MonitoredService.builder()
                                            .accountId("testAccountId")
                                            .orgIdentifier("testOrg")
                                            .projectIdentifier("testProject")
                                            .serviceIdentifier("testService")
                                            .environmentIdentifierList(environmentIdentifierList)
                                            .identifier("testService_testEnvironment")
                                            .build();
    final String emailMonitoredServiceNameHyperlink =
        getEmailMonitoredServiceNameHyperlink(TEST_BASE_URL, monitoredService);
    assert emailMonitoredServiceNameHyperlink.equals(
        "<a style=\"text-decoration: none; color: #0278D5;\" href=\"https://testurl.com/account/testAccountId/cet/orgs/testOrg/projects/testProject/etmonitoredservices/edit/testService_testEnvironment\">testService_testEnvironment</a>");
  }

  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void getEmailNotificationLinkTest() {
    List<String> environmentIdentifierList = Collections.singletonList(TEST_ENVIRONMENT_ID);

    MonitoredService monitoredService = MonitoredService.builder()
                                            .accountId("testAccountId")
                                            .orgIdentifier("testOrg")
                                            .projectIdentifier("testProject")
                                            .serviceIdentifier("testService")
                                            .environmentIdentifierList(environmentIdentifierList)
                                            .identifier("testService_testEnvironment")
                                            .build();
    final MonitoredServiceCodeErrorCondition codeErrorCondition = MonitoredServiceCodeErrorCondition.builder().build();
    List<MonitoredServiceNotificationRuleCondition> codeErrorConditions = Collections.singletonList(codeErrorCondition);
    final MonitoredServiceNotificationRule notificationRule =
        MonitoredServiceNotificationRule.builder().conditions(codeErrorConditions).name("testNotificationRule").build();
    final String emailNotificationLink = getEmailNotificationLink(TEST_BASE_URL, monitoredService, notificationRule);
    assert emailNotificationLink.equals(
        "<a style=\"text-decoration: none; color: #0278D5;\" href=\"https://testurl.com/account/testAccountId/cet/orgs/testOrg/projects/testProject/etmonitoredservices/edit/testService_testEnvironment\">testNotificationRule</a>");
  }
}
