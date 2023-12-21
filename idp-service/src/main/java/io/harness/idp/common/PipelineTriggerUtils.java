/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.common;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.retry.RetryHelper;

import io.github.resilience4j.retry.Retry;
import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.http2.ConnectionShutdownException;
import okhttp3.internal.http2.StreamResetException;

@OwnedBy(HarnessTeam.IDP)
@UtilityClass
@Slf4j
public class PipelineTriggerUtils {
  public static final String ACCOUNT_ID = "account_id";
  public static final String NAMESPACE = "namespace";
  private static final MediaType APPLICATION_JSON = MediaType.parse("application/json");

  public static Retry buildRetryAndRegisterListeners(String className) {
    final Retry exponentialRetry = RetryHelper.getExponentialRetry(className,
        new Class[] {ConnectException.class, TimeoutException.class, ConnectionShutdownException.class,
            StreamResetException.class});
    RetryHelper.registerEventListeners(exponentialRetry);
    return exponentialRetry;
  }

  public void trigger(String accountIdentifier, String namespace, String url, Retry retry) {
    Request request = createHttpRequest(accountIdentifier, namespace, url);
    OkHttpClient client = new OkHttpClient();
    Supplier<Response> response = Retry.decorateSupplier(retry, () -> {
      try {
        return client.newCall(request).execute();
      } catch (IOException e) {
        String errMessage = "Error occurred while reaching pipeline trigger API. "
            + "Account: " + accountIdentifier + ". URL: " + url;
        log.error(errMessage, e);
        throw new InvalidRequestException(errMessage);
      }
    });

    if (!response.get().isSuccessful()) {
      throw new InvalidRequestException("Pipeline Trigger http call failed. "
          + "Account: " + accountIdentifier + ". URL: " + url);
    }
  }

  private Request createHttpRequest(String accountIdentifier, String namespace, String url) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(ACCOUNT_ID, accountIdentifier);
    jsonObject.put(NAMESPACE, namespace);

    RequestBody requestBody = RequestBody.create(jsonObject.toString(), APPLICATION_JSON);

    return new Request.Builder().url(url).post(requestBody).build();
  }
}
