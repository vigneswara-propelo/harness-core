/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.exceptionhandler.handler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.k8s.exception.KubernetesExceptionHints.K8S_API_FORBIDDEN_EXCEPTION;
import static io.harness.k8s.exception.KubernetesExceptionHints.K8S_KUBECTL_AUTH_DEFAULT_HINT;
import static io.harness.rule.OwnerRule.MLUKIC;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.exception.KubernetesApiExceptionHandler;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.KubernetesApiTaskException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import io.kubernetes.client.openapi.ApiException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class KubernetesApiExceptionHandlerTest extends CategoryTest {
  private KubernetesApiExceptionHandler exceptionHandler = new KubernetesApiExceptionHandler();
  private static final String KUBECTL_AUTH_HINT_TEMPLATE =
      "kubectl --kubeconfig=${HARNESS_KUBE_CONFIG_PATH} auth can-i %s %s --as=%s --namespace=%s";

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testK8sAuthException() {
    testK8sAuthException(
        "{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"configmaps \\\"release-1\\\" is forbidden: User \\\"system:serviceaccount:harness:default\\\" cannot get resource \\\"configmaps\\\" in API group \\\"\\\" in the namespace \\\"test\\\"\",\"reason\":\"Forbidden\",\"details\":{\"name\":\"release-1\",\"kind\":\"configmaps\"},\"code\":403}",
        format(KUBECTL_AUTH_HINT_TEMPLATE, "get", "configmaps", "system:serviceaccount:harness:default", "test"));
    testK8sAuthException(
        "{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"secrets \\\"release-1\\\" is forbidden: User \\\"system:serviceaccount:harness:default\\\" cannot list resource \\\"secrets\\\" in API group \\\"\\\" in the namespace \\\"test\\\"\",\"reason\":\"Forbidden\",\"details\":{\"name\":\"release-1\",\"kind\":\"secrets\"},\"code\":403}",
        format(KUBECTL_AUTH_HINT_TEMPLATE, "list", "secrets", "system:serviceaccount:harness:default", "test"));
    testK8sAuthException(
        "{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"configmaps \\\"release-1\\\" is forbidden: User \\\"system:serviceaccount:harness:default\\\" cannot create resource \\\"configmaps\\\" in API group \\\"apps\\\" in the namespace \\\"testns\\\"\",\"reason\":\"Forbidden\",\"details\":{\"name\":\"release-1\",\"kind\":\"configmaps\"},\"code\":403}",
        format(KUBECTL_AUTH_HINT_TEMPLATE, "create", "configmaps", "system:serviceaccount:harness:default", "testns"));
    testK8sAuthException(
        "{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"configmaps \\\"release-1\\\" is forbidden: User \\\"system:serviceaccount:harness:default\\\" cannot delete resource \\\"configmaps\\\" in API group \\\"apps\\\" in the namespace \\\"testns\\\"\",\"reason\":\"Forbidden\",\"details\":{\"name\":\"release-1\",\"kind\":\"configmaps\"},\"code\":403}",
        format(KUBECTL_AUTH_HINT_TEMPLATE, "delete", "configmaps", "system:serviceaccount:harness:default", "testns"));
    testK8sAuthException(
        "{\"kind\":\"Status\",\"apiVersion\":\"v1\",\"metadata\":{},\"status\":\"Failure\",\"message\":\"configmaps \\\"release-1\\\" is forbidden: User \\\"\\\" cannot delete resource \\\"\\\" in API group \\\"apps\\\" in the namespace \\\"\\\"\",\"reason\":\"Forbidden\",\"details\":{\"name\":\"release-1\",\"kind\":\"configmaps\"},\"code\":403}",
        format("%s%n%s", K8S_API_FORBIDDEN_EXCEPTION, K8S_KUBECTL_AUTH_DEFAULT_HINT));
    testK8sAuthException(
        "some other error message", format("%s%n%s", K8S_API_FORBIDDEN_EXCEPTION, K8S_KUBECTL_AUTH_DEFAULT_HINT));
  }

  private void testK8sAuthException(String errorMessage, String resultMessage) {
    ApiException apiException = new ApiException("Forbidden", 403, null, errorMessage);
    WingsException wingsException = exceptionHandler.handleException(apiException);

    assertThat(wingsException.getClass()).isNotNull();
    assertThat(wingsException).isInstanceOf(HintException.class);
    assertThat(wingsException.getCause()).isNotNull();
    assertThat(wingsException.getCause()).isInstanceOf(ExplanationException.class);
    assertThat(wingsException.getCause().getCause()).isNotNull();
    assertThat(wingsException.getCause().getCause()).isInstanceOf(KubernetesApiTaskException.class);

    assertThat(wingsException.getMessage()).contains(resultMessage);
  }
}
