package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;

@OwnedBy(PL)
public class ScmBadRequestException extends ScmException {
  public ScmBadRequestException(String errorMessage) {
    super(errorMessage, ErrorCode.SCM_BAD_REQUEST);
  }
}
