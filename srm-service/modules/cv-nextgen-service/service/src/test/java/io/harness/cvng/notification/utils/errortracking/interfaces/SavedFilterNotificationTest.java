/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.cvng.notification.utils.errortracking.interfaces;

import static io.harness.rule.OwnerRule.JAMES_RICKS;

import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.errortracking.CriticalEventType;
import io.harness.cvng.beans.errortracking.ErrorTrackingNotificationData;
import io.harness.cvng.beans.errortracking.EventStatus;
import io.harness.cvng.beans.errortracking.SavedFilter;
import io.harness.rule.Owner;

import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SavedFilterNotificationTest {
  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void getEventStatusTest() {
    final List<EventStatus> eventStatus = List.of(EventStatus.NEW_EVENTS, EventStatus.CRITICAL_EVENTS);

    final SavedFilter savedFilter = SavedFilter.builder().statuses(eventStatus).build();

    ErrorTrackingNotificationData errorTrackingNotificationData =
        ErrorTrackingNotificationData.builder().filter(savedFilter).build();
    final String eventStatusString = SavedFilterNotification.getEventStatus(errorTrackingNotificationData);
    assert eventStatusString.equals("New Events, Critical Events");
  }

  @Test
  @Owner(developers = JAMES_RICKS)
  @Category(UnitTests.class)
  public void getNotificationEventTriggerListTest() {
    final SavedFilter savedFilter =
        SavedFilter.builder()
            .eventTypes(List.of(CriticalEventType.SWALLOWED_EXCEPTION, CriticalEventType.LOGGED_ERROR))
            .searchTerm("testSearchTerm")
            .build();

    ErrorTrackingNotificationData errorTrackingNotificationData =
        ErrorTrackingNotificationData.builder().filter(savedFilter).build();
    final String eventTriggerList =
        SavedFilterNotification.getNotificationEventTriggerList(errorTrackingNotificationData);
    assert eventTriggerList.equals("Swallowed Exceptions, Logged Errors, and search term (testSearchTerm)");
  }
}
