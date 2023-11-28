/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.service.core.litek8s;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.logging.LogLevel;

import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.util.Watch;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RunnerK8EventHandler {
  final private CoreV1Api coreApi;

  private static Integer watchTimeout = 8 * 60;

  public Watch<CoreV1Event> startAsyncPodEventWatch(
      String namespace, String pod, ILogStreamingTaskClient logStreamingTaskClient) {
    String fieldSelector = String.format("involvedObject.name=%s,involvedObject.kind=Pod", pod);
    try {
      Watch<CoreV1Event> watch = createWatch(namespace, fieldSelector);
      new Thread(() -> {
        try {
          logWatchEvents(watch, logStreamingTaskClient);
        } catch (IOException e) {
          log.warn("error in watching pod events", e);
        }
      }).start();
      return watch;

    } catch (ApiException e) {
      logStreamingTaskClient.log(LogLevel.ERROR, String.format("failed to watch pod event: %s", e.getMessage()));
      log.error("failed to create watch on pod events", e);
      return null;
    }
  }

  public void stopEventWatch(Watch<CoreV1Event> watch) {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.submit(() -> {
      try {
        watch.close();
      } catch (IOException e) {
        log.warn("failed to stop event watch", e);
      }
    });
    try {
      executorService.shutdown();
      executorService.awaitTermination(1, TimeUnit.SECONDS);
    } catch (Exception e) {
      log.warn("failed to stop event watch", e);
    }
  }

  private Watch<CoreV1Event> createWatch(String namespace, String fieldSelector) throws ApiException {
    return Watch.createWatch(coreApi.getApiClient(),
        coreApi.listNamespacedEventCall(
            namespace, null, false, null, fieldSelector, null, null, null, null, watchTimeout, Boolean.TRUE, null),
        new TypeToken<Watch.Response<CoreV1Event>>() {}.getType());
  }

  private void logWatchEvents(Watch<CoreV1Event> watch, ILogStreamingTaskClient logStreamingTaskClient)
      throws IOException {
    try {
      for (Watch.Response<CoreV1Event> item : watch) {
        if (item != null && item.object != null && isNotEmpty(item.object.getMessage())) {
          logStreamingTaskClient.log(getLogLevel(item.object.getType()), item.object.getMessage());
          log.info(
              "{}: Event- {}, Reason - {}", item.object.getType(), item.object.getMessage(), item.object.getReason());
        }
      }
    } catch (Exception ex) {
      log.warn("Unable to watch pod event with exception ", ex);
    } finally {
      watch.close();
    }
  }

  private LogLevel getLogLevel(String eventType) {
    if (eventType.equals("WARNING")) {
      return LogLevel.WARN;
    }
    return LogLevel.INFO;
  }
}
