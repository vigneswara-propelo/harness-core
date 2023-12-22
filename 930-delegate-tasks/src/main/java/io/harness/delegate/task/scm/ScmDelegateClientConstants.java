/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.scm;

import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ScmDelegateClientConstants {
  public final String SCM_SERVER_CORE_POOL_SIZE_ENV_VAR = "SCM_SERVER_CORE_POOL_SIZE";
  public final String SCM_SERVER_MAX_POOL_SIZE_ENV_VAR = "SCM_SERVER_MAX_POOL_SIZE";
  public final String SCM_SERVER_POOL_IDLE_TIME_IN_SECONDS_ENV_VAR = "SCM_SERVER_POOL_IDLE_TIME_SECONDS";
  public final int DEFAULT_SCM_SERVER_CORE_POOL_SIZE = 1;
  public final int DEFAULT_SCM_SERVER_MAX_POOL_SIZE = 10;
  public final int DEFAULT_SCM_SERVER_POOL_IDLE_TIME_VALUE = 60;
  public final TimeUnit DEFAULT_SCM_SERVER_POOL_IDLE_TIME_UNIT = TimeUnit.SECONDS;
}
