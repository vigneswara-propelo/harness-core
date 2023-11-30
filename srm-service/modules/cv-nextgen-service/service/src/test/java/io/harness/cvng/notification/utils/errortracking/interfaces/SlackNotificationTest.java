/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.cvng.notification.utils.errortracking.interfaces;

import static io.harness.cvng.notification.utils.errortracking.interfaces.AggregatedNotificationTest.TEST_BASE_URL;
import static io.harness.cvng.notification.utils.errortracking.interfaces.AggregatedNotificationTest.getTestCodeErrorCondition;
import static io.harness.cvng.notification.utils.errortracking.interfaces.AggregatedNotificationTest.getTestNotificationData;
import static io.harness.rule.OwnerRule.JAMES_RICKS;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SlackNotificationTest {
  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void getSlackFormattedVersionListTest() {
    final String slackFormattedVersionList = SlackNotification.getSlackFormattedVersionList(
        getTestCodeErrorCondition(), getTestNotificationData(), TEST_BASE_URL);
    assert slackFormattedVersionList.equals("Events appeared on the deployment version *testVersion*\n"
        + "<https://testurl.com/account/testAccountId/cet/orgs/testOrg/projects/testProject/eventsummary/events?env=testEnvironmentId&service=testService&dep=testVersion&fromTimestamp=1700258695&toTimestamp=1700258701&eventStatus=NewEvents|New Events (1)>   |   Critical Events (0)   |   Resurfaced Events (0)");
  }
}
