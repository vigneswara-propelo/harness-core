/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.cv;

import java.time.Duration;

public class CVConstants {
  private CVConstants() {}
  public static Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);
  public static final Duration DATA_COLLECTION_RETRY_SLEEP = Duration.ofSeconds(30);
  public static final int DELAY_MINUTES = 2;
  public static final int DURATION_TO_ASK_MINUTES = 5;
  public static final long ML_RECORDS_TTL_MONTHS = 6;
  public static final String DELEGATE_DATA_COLLECTION = "delegate-data-collection";
  public static final String CV_TASK_STATUS_UPDATE_PATH = "/update-cv-task-status";
  public static final String SAVE_CV_ACTIVITY_LOGS_PATH = "/save-cv-activity-logs";
  public static final int MAX_RETRIES = 2;
  public static final int RATE_LIMIT_STATUS = 429;
  public static final String URL_STRING = "Url";
  public static final String CONNECTOR = ":";
  public static final String TEST_HOST_NAME = "testNode";
  public static final String CONTROL_HOST_NAME = "controlNode";
  public static final int CANARY_DAYS_TO_COLLECT = 7;
  public static final String DEFAULT_TIME_ZONE = "UTC";
  public static final String DUMMY_HOST_NAME = "dummy";
  public static final long TOTAL_HITS_PER_MIN_THRESHOLD = 1000;
  public static String AZURE_BASE_URL = "https://api.loganalytics.io/";
  public static String AZURE_TOKEN_URL = "https://login.microsoftonline.com/";
  public static final int CRON_POLL_INTERVAL_IN_MINUTES = 15;
  public static final int CV_DATA_COLLECTION_INTERVAL_IN_MINUTE = CRON_POLL_INTERVAL_IN_MINUTES / 3;
  public static final String STACKDRIVER_DEFAULT_LOG_MESSAGE_FIELD = "textPayload";
  public static final String STACKDRIVER_DEFAULT_HOST_NAME_FIELD = "pod_id";
  public static final String STACK_DRIVER_QUERY_SEPARATER = " AND ";
}
