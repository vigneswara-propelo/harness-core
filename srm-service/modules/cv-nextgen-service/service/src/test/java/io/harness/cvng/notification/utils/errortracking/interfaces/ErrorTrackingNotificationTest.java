/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.cvng.notification.utils.errortracking.interfaces;

import static io.harness.cvng.notification.utils.errortracking.interfaces.AggregatedNotificationTest.TEST_BASE_URL;
import static io.harness.cvng.notification.utils.errortracking.interfaces.AggregatedNotificationTest.TEST_ENVIRONMENT_ID;
import static io.harness.cvng.notification.utils.errortracking.interfaces.ErrorTrackingNotification.ENVIRONMENT_NAME;
import static io.harness.cvng.notification.utils.errortracking.interfaces.ErrorTrackingNotification.validateTemplateValues;
import static io.harness.rule.OwnerRule.JAMES_RICKS;

import io.harness.category.element.UnitTests;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ErrorTrackingNotificationTest {
  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void getNotificationUrlTest() {
    List<String> environmentIdentifierList = Collections.singletonList(TEST_ENVIRONMENT_ID);

    MonitoredService monitoredService = MonitoredService.builder()
                                            .accountId("testAccountId")
                                            .orgIdentifier("testOrg")
                                            .projectIdentifier("testProject")
                                            .serviceIdentifier("testService")
                                            .environmentIdentifierList(environmentIdentifierList)
                                            .identifier("testService_testEnvironment")
                                            .build();
    final String notificationUrl = ErrorTrackingNotification.getNotificationUrl(TEST_BASE_URL, monitoredService);
    assert notificationUrl.equals(
        "https://testurl.com/account/testAccountId/cet/orgs/testOrg/projects/testProject/etmonitoredservices/edit/testService_testEnvironment");
  }

  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void validateTemplateValuesNonEmptyTest() {
    Map<String, String> templateData = Collections.singletonMap(ENVIRONMENT_NAME, "environmentName");
    validateTemplateValues(templateData);
  }

  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void validateTemplateValuesBlankExcludedKeyTest() {
    Map<String, String> templateData = Collections.singletonMap(ENVIRONMENT_NAME, "");
    validateTemplateValues(templateData, ENVIRONMENT_NAME);
  }

  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void validateTemplateValuesNullExcludedKeyTest() {
    Map<String, String> templateData = Collections.singletonMap(ENVIRONMENT_NAME, null);
    validateTemplateValues(templateData, ENVIRONMENT_NAME);
  }
  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void validateTemplateValuesBlankTest() {
    Map<String, String> templateData = Collections.singletonMap(ENVIRONMENT_NAME, "");
    validateTemplateValues(templateData);
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void validateTemplateValuesNullTest() {
    Map<String, String> templateData = Collections.singletonMap(ENVIRONMENT_NAME, null);
    validateTemplateValues(templateData);
  }
}
