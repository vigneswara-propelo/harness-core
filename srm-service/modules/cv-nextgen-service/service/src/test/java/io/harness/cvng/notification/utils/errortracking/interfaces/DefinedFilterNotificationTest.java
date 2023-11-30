/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.cvng.notification.utils.errortracking.interfaces;

import static io.harness.rule.OwnerRule.JAMES_RICKS;

import io.harness.category.element.UnitTests;
import io.harness.cvng.notification.beans.ErrorTrackingEventStatus;
import io.harness.cvng.notification.beans.ErrorTrackingEventType;
import io.harness.rule.Owner;

import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DefinedFilterNotificationTest {
  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void getEventStatusTest() {
    List<ErrorTrackingEventStatus> errorTrackingEventStatus = List.of(ErrorTrackingEventStatus.values());
    final String eventStatus = DefinedFilterNotification.getEventStatus(errorTrackingEventStatus);
    assert eventStatus.equals("New Events, Critical Events, Resurfaced Events");
  }

  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void getNotificationEventTriggerListTest() {
    List<ErrorTrackingEventType> eventTypeList = List.of(ErrorTrackingEventType.values());
    final String eventTypes = DefinedFilterNotification.getNotificationEventTriggerList(eventTypeList);
    assert eventTypes.equals(
        "Exceptions, Log Errors, Http Errors, Custom Errors, Timeout Errors, Swallowed Exceptions, Caught Exceptions, Uncaught Exceptions, Log Warnings");
  }
}
