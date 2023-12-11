/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.chaos;

import static io.harness.annotations.dev.HarnessTeam.CHAOS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.notification.Team;
import io.harness.notification.notificationclient.NotificationClient;
import io.harness.notification.templates.PredefinedTemplate;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CHAOS)
@Singleton
@Slf4j
public class ChaosNotificationTemplateRegistrar implements Runnable {
  @Inject NotificationClient notificationClient;

  @Override
  public void run() {
    try {
      int timout = 1;
      List<PredefinedTemplate> templates = new ArrayList<>(Arrays.asList(
          PredefinedTemplate.CHAOS_EXPERIMENT_STARTED_EMAIL, PredefinedTemplate.CHAOS_EXPERIMENT_STARTED_MSTEAMS,
          PredefinedTemplate.CHAOS_EXPERIMENT_STARTED_PAGERDUTY, PredefinedTemplate.CHAOS_EXPERIMENT_STARTED_SLACK,
          PredefinedTemplate.CHAOS_EXPERIMENT_STARTED_WEBHOOK, PredefinedTemplate.CHAOS_EXPERIMENT_COMPLETED_EMAIL,
          PredefinedTemplate.CHAOS_EXPERIMENT_COMPLETED_MSTEAMS,
          PredefinedTemplate.CHAOS_EXPERIMENT_COMPLETED_PAGERDUTY, PredefinedTemplate.CHAOS_EXPERIMENT_COMPLETED_SLACK,
          PredefinedTemplate.CHAOS_EXPERIMENT_COMPLETED_WEBHOOK, PredefinedTemplate.CHAOS_EXPERIMENT_STOPPED_EMAIL,
          PredefinedTemplate.CHAOS_EXPERIMENT_STOPPED_MSTEAMS, PredefinedTemplate.CHAOS_EXPERIMENT_STOPPED_PAGERDUTY,
          PredefinedTemplate.CHAOS_EXPERIMENT_STOPPED_SLACK, PredefinedTemplate.CHAOS_EXPERIMENT_STOPPED_WEBHOOK));
      while (true) {
        List<PredefinedTemplate> unprocessedTemplate = new ArrayList<>();
        for (PredefinedTemplate template : templates) {
          log.info("Registering {} with NotificationService", template);
          try {
            notificationClient.saveNotificationTemplate(Team.OTHER, template, true);
          } catch (Exception ex) {
            log.error(String.format("Unable to register template with id: %s", template.getIdentifier()), ex);
            unprocessedTemplate.add(template);
          }
        }
        if (unprocessedTemplate.isEmpty()) {
          break;
        }

        Thread.sleep(timout);

        timout *= 10;
        templates = unprocessedTemplate;
      }
    } catch (InterruptedException e) {
      log.error("", e);
    }
  }
}
