package io.harness.delegate.exceptionhandler.handler;

import static io.harness.rule.OwnerRule.DEV_MITTAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.runtime.SCMRuntimeException;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SCMExceptionHandlerTest extends CategoryTest {
  SCMExceptionHandler scmExceptionHandler = new SCMExceptionHandler();
  Exception exception;

  @Test
  @Owner(developers = DEV_MITTAL)
  @Category(UnitTests.class)
  public void testSCMUnauthorised() {
    exception = new SCMRuntimeException("blah", ErrorCode.SCM_UNAUTHORIZED);
    Exception actual = scmExceptionHandler.handleException(exception);
    assertThat(actual instanceof HintException).isTrue();
    assertThat(actual.getCause() instanceof ExplanationException).isTrue();
    assertThat(actual.getMessage()).isEqualTo(HintException.HINT_INVALID_GIT_API_AUTHORIZATION);
    assertThat(actual.getCause().getCause() instanceof InvalidRequestException).isTrue();
  }

  @Test
  @Owner(developers = DEV_MITTAL)
  @Category(UnitTests.class)
  public void testSCMInvalidRequest() {
    exception = new SCMRuntimeException("blah", ErrorCode.INVALID_REQUEST);
    Exception actual = scmExceptionHandler.handleException(exception);
    assertThat(actual instanceof HintException).isTrue();
    assertThat(actual.getMessage()).isEqualTo(HintException.HINT_SCM_INVALID_REQUEST);
    assertThat(actual.getCause() instanceof ExplanationException).isTrue();
    assertThat(actual.getCause().getCause() instanceof InvalidRequestException).isTrue();
  }

  @Test
  @Owner(developers = DEV_MITTAL)
  @Category(UnitTests.class)
  public void testSCMUnexpectedErrorCode() {
    exception = new SCMRuntimeException("blah", ErrorCode.UNEXPECTED);
    Exception actual = scmExceptionHandler.handleException(exception);
    assertThat(actual instanceof InvalidRequestException).isTrue();
    assertThat(actual.getMessage()).isEqualTo("blah");
  }
}
