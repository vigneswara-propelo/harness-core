package io.harness.gitsync.common.scmerrorhandling.handlers.github;

import static io.harness.rule.OwnerRule.BHAVYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.ScmResourceNotFoundException;
import io.harness.exception.ScmUnauthorizedException;
import io.harness.exception.ScmUnexpectedException;
import io.harness.exception.ScmUnprocessableEntityException;
import io.harness.exception.WingsException;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class GithubCreateBranchScmApiErrorHandlerTest extends GitSyncTestBase {
  @Inject GithubCreateBranchScmApiErrorHandler githubCreateBranchScmApiErrorHandler;

  private static final String errorMessage = "errorMessage";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testHandleErrorOnUnauthorizedResponse() {
    try {
      githubCreateBranchScmApiErrorHandler.handleError(401, errorMessage);
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmUnauthorizedException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testHandleErrorOnUnauthenticatedResponse() {
    try {
      githubCreateBranchScmApiErrorHandler.handleError(403, errorMessage);
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmUnauthorizedException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testHandleErrorOnResourceNotFoundResponse() {
    try {
      githubCreateBranchScmApiErrorHandler.handleError(404, errorMessage);
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmResourceNotFoundException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testHandleErrorOnUnprocessableEntityResponse() {
    try {
      githubCreateBranchScmApiErrorHandler.handleError(422, errorMessage);
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmUnprocessableEntityException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testHandleErrorWhenUnexpectedStatusCode() {
    try {
      githubCreateBranchScmApiErrorHandler.handleError(405, errorMessage);
    } catch (Exception ex) {
      WingsException exception = ExceptionUtils.cause(ScmUnexpectedException.class, ex);
      assertThat(exception).isNotNull();
      assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
  }
}
