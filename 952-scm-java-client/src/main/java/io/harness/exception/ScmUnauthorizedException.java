package io.harness.exception;

import io.harness.eraro.ErrorCode;

public class ScmUnauthorizedException extends ScmException {
  public ScmUnauthorizedException(String errorMessage) {
    super(errorMessage, null, ErrorCode.SCM_UNAUTHORIZED);
  }
}
