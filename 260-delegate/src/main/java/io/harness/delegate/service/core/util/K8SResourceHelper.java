/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.core.util;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.exception.PodNotFoundException;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodCondition;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class K8SResourceHelper {
  private static final Pattern RESOURCE_NAME_NORMALIZER = Pattern.compile("_|/");
  private static final int POD_WAIT_UNTIL_READY_SLEEP_SECS = 2;
  private static final String POD_PENDING_PHASE = "Pending";
  private static final String POD_RUNNING_PHASE = "Running";

  @NonNull
  public static String getPodName(final String taskGroupId) {
    return normalizeResourceName("harness-" + taskGroupId + "-job");
  }

  @NonNull
  public static String getContainerName(final String taskId) {
    return normalizeResourceName("task-" + taskId);
  }

  @NonNull
  public static String getImagePullSecretName(final String taskGroupId, final long index) {
    return normalizeResourceName(String.format("image-%s-%s", taskGroupId, index));
  }

  @NonNull
  public static String getSecretName(final String taskId) {
    return normalizeResourceName("secret-" + taskId);
  }

  // K8S resource name needs to contain only lowercase alphanumerics . and -, but must start and end with alphanumerics
  // Regex used by K8S for validation is '[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*'
  @NonNull
  public static String normalizeResourceName(final String resourceName) {
    return RESOURCE_NAME_NORMALIZER.matcher(resourceName.trim().toLowerCase(Locale.ROOT)).replaceAll("-");
  }

  public void waitUntilPodIsReady(CoreV1Api coreV1Api, String podName, String namespace, int podMaxWaitUntilReadySecs)
      throws PodCreationFailedException, InterruptedException {
    int errorCounter = 0;
    V1Pod pod = null;
    Instant startTime = Instant.now();
    Instant currTime = startTime;

    while (Duration.between(startTime, currTime).getSeconds() < podMaxWaitUntilReadySecs) {
      // Either pod is in pending phase where it is waiting for scheduling / creation of containers
      // or pod is waiting for containers to move to running state.
      if (pod != null && !isPodInPendingPhase(pod) && !isPodInWaitingState(pod) && isIpAssigned(pod)) {
        return;
      }

      Thread.sleep(POD_WAIT_UNTIL_READY_SLEEP_SECS * 1000L);
      try {
        pod = getPod(coreV1Api, podName, namespace);
      } catch (Exception ex) {
        errorCounter = errorCounter + 1;
        log.error("Pod get call failed, errorCounter: {}", errorCounter, ex);
        if (errorCounter >= 10) {
          throw new PodNotFoundException(format("Pod %s is not present in namespace %s", podName, namespace), ex);
        }
        continue;
      }
      currTime = Instant.now();
    }

    String errMsg;
    // If pod's container status list is non-empty, reason for pod not to be in running state is in waiting container's
    // status message. Else reason is present in pod conditions.
    if (isNotEmpty(pod.getStatus().getContainerStatuses())) {
      List<String> containerErrs =
          pod.getStatus()
              .getContainerStatuses()
              .stream()
              .filter(containerStatus -> containerStatus.getState().getWaiting() != null)
              .filter(containerStatus -> containerStatus.getState().getWaiting().getMessage() != null)
              .map(containerStatus -> containerStatus.getState().getWaiting().getMessage())
              .collect(Collectors.toList());
      errMsg = String.join(", ", containerErrs);
    } else {
      List<String> podConditions = pod.getStatus()
                                       .getConditions()
                                       .stream()
                                       .filter(Objects::nonNull)
                                       .map(V1PodCondition::getMessage)
                                       .collect(Collectors.toList());
      errMsg = String.join(", ", podConditions);
    }

    // If we are not able to get exact errMsg from container status or pod conditions then we throw exception with
    // generic error message.
    if (isEmpty(errMsg)) {
      errMsg = format("Timeout exception: Pod containers failed to reach running state within %s seconds",
          podMaxWaitUntilReadySecs);
    }
    throw new PodCreationFailedException(String.format("Pod creation failed with error message: %s", errMsg));
  }

  public V1Pod getPod(CoreV1Api coreV1Api, String podName, String namespace) throws ApiException {
    return coreV1Api.readNamespacedPod(podName, namespace, null);
  }

  private boolean isPodInWaitingState(V1Pod pod) {
    for (V1ContainerStatus containerStatus : pod.getStatus().getContainerStatuses()) {
      if (containerStatus.getState().getWaiting() != null) {
        return true;
      }
    }
    return false;
  }

  private boolean isIpAssigned(V1Pod pod) {
    if (pod.getStatus().getPodIP() != null) {
      return true;
    }
    return false;
  }

  private boolean isPodInPendingPhase(V1Pod pod) {
    String podPhase = pod.getStatus().getPhase();
    return podPhase.equals(POD_PENDING_PHASE);
  }
}
