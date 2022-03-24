/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.statemachine.services.api;

import static io.harness.cvng.analysis.CVAnalysisConstants.MAX_RETRIES;

import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;

import com.google.common.collect.Lists;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public abstract class AnalysisStateExecutor<T extends AnalysisState> {
  private static final List<Duration> RETRY_WAIT_DURATIONS = Lists.newArrayList(Duration.ofSeconds(5),
      Duration.ofSeconds(10), Duration.ofSeconds(20), Duration.ofSeconds(30), Duration.ofMinutes(1));

  public abstract AnalysisState execute(T analysisState);
  public abstract AnalysisStatus getExecutionStatus(T analysisState);
  public abstract AnalysisState handleRerun(T analysisState);
  public abstract AnalysisState handleRunning(T analysisState);
  public abstract AnalysisState handleSuccess(T analysisState);
  public abstract AnalysisState handleTransition(T analysisState);
  public abstract AnalysisState handleRetry(T analysisState);
  public void handleFinalStatuses(T analysisState) {
    // no-op - designed to override
  }

  public AnalysisState handleFailure(T analysisState) {
    analysisState.setStatus(AnalysisStatus.FAILED);
    return analysisState;
  }

  public AnalysisState handleTimeout(T analysisState) {
    analysisState.setStatus(AnalysisStatus.TIMEOUT);
    return analysisState;
  }

  public int getMaxRetry() {
    return MAX_RETRIES;
  }

  public Instant getNextValidAfter(Instant currentTime, int retryCount) {
    return currentTime.plus(RETRY_WAIT_DURATIONS.get(Math.min(retryCount, RETRY_WAIT_DURATIONS.size() - 1)));
  }
}
