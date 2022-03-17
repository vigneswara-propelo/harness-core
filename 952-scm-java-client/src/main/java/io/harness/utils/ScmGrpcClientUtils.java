package io.harness.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ConnectException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.GeneralException;
import io.harness.exception.WingsException;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@UtilityClass
@Slf4j
@OwnedBy(PL)
public class ScmGrpcClientUtils {
  private static final RetryPolicy<Object> RETRY_POLICY = createRetryPolicy();

  public <T, R> R retryAndProcessException(Function<T, R> fn, T arg) {
    try {
      return Failsafe.with(RETRY_POLICY).get(() -> fn.apply(arg));
    } catch (Exception ex) {
      throw processException(ex);
    }
  }

  private WingsException processException(Exception ex) {
    if (ex instanceof WingsException) {
      return (WingsException) ex;
    } else if (ex instanceof StatusRuntimeException) {
      log.error("Unable to connect to Git Provider, error while connecting to scm service", ex);
      return new ConnectException(
          "Unable to connect to Git Provider, Please retry after sometime.", WingsException.USER);
    } else {
      log.error("Error connecting to scm service", ex);
      return new GeneralException(
          ex == null ? "Unknown error while communicating with scm service" : ExceptionUtils.getMessage(ex));
    }
  }

  private RetryPolicy<Object> createRetryPolicy() {
    return new RetryPolicy<>()
        .withDelay(Duration.ofMillis(750))
        .withMaxAttempts(1)
        .onFailedAttempt(event
            -> log.warn(String.format("Scm grpc retry attempt: %d", event.getAttemptCount()), event.getLastFailure()))
        .handleIf(throwable -> {
          if (!(throwable instanceof StatusRuntimeException)) {
            return false;
          }
          StatusRuntimeException statusRuntimeException = (StatusRuntimeException) throwable;
          return statusRuntimeException.getStatus().getCode() == Status.Code.UNAVAILABLE
              || statusRuntimeException.getStatus().getCode() == Status.Code.UNKNOWN;
        });
  }
}
