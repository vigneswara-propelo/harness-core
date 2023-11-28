/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.util;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;

@OwnedBy(CE)
@Slf4j
public class EventServiceRestUtils {
  private static final int MAX_ATTEMPTS = 3;

  public static <T> T executeRestCallWithRetry(Call<T> call) throws IOException {
    int attempt = 0;
    while (attempt < MAX_ATTEMPTS) {
      attempt++;
      try {
        Response<T> response = call.clone().execute();
        log.info("Rest call to CE Event Service isSuccessful: {}", response.isSuccessful());
        if (response.isSuccessful()) {
          return response.body();
        } else {
          String errorResponse = (response.errorBody() != null) ? response.errorBody().string() : "";
          final int errorCode = response.code();
          throw new IOException(
              String.format("CE Event Rest call received %d Error Response: %s", errorCode, errorResponse));
        }
      } catch (Exception e) {
        if (attempt <= MAX_ATTEMPTS) {
          log.warn("Error executing rest call {}, retrying..., attempt {}", e, attempt);
        } else {
          log.error("Error executing rest call", e);
          throw new IOException(e);
        }
      }
    }
    return null;
  }
}
