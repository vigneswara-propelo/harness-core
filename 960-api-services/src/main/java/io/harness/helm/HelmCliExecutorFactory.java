/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.helm;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.threading.ThreadPool;
import io.harness.utils.system.SystemWrapper;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false, components = {HarnessModuleComponent.CDS_K8S})
public class HelmCliExecutorFactory {
  private static final String HELM_CLI_CORE_POOL_SIZE_ENV = "HELM_CLI_CORE_POOL_SIZE";
  private static final String HELM_CLI_MAX_POOL_SIZE_ENV = "HELM_CLI_MAX_POOL_SIZE";

  private static final int DEFAULT_CORE_POOL_SIZE = 1;
  private static final int DEFAULT_MAX_CORE_POOL_SIZE = 10;

  public static ExecutorService create() {
    return ThreadPool.create(getCorePoolSize(), getMaxPoolSize(), 5, TimeUnit.SECONDS,
        new ThreadFactoryBuilder().setNameFormat("helm-cli-%d").setPriority(Thread.MIN_PRIORITY).build());
  }

  private static int getCorePoolSize() {
    return SystemWrapper.getOrDefaultInt(HELM_CLI_CORE_POOL_SIZE_ENV, DEFAULT_CORE_POOL_SIZE);
  }

  private static int getMaxPoolSize() {
    return SystemWrapper.getOrDefaultInt(HELM_CLI_MAX_POOL_SIZE_ENV, DEFAULT_MAX_CORE_POOL_SIZE);
  }
}
