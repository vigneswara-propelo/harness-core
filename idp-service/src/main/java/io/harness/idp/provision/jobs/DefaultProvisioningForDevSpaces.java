/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.provision.jobs;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.namespace.service.NamespaceService;
import io.harness.idp.provision.service.ProvisionService;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.IDP)
public class DefaultProvisioningForDevSpaces implements Managed {
  @Inject @Named("env") private String env;
  @Inject @Named("devSpaceDefaultBackstageNamespace") private String devSpaceDefaultBackstageNamespace;

  @Inject @Named("devSpaceDefaultAccountId") private String devSpaceDefaultAccountId;

  private ExecutorService executorService;
  private final NamespaceService namespaceService;
  private final ProvisionService provisionService;

  private static final String DEV_SPACE_ENV_TYPE = "dev-spaces";

  @Inject
  public DefaultProvisioningForDevSpaces(@Named("DefaultDevSpaceEnvProvisioner") ExecutorService executorService,
      NamespaceService namespaceService, ProvisionService provisionService) {
    this.executorService = executorService;
    this.namespaceService = namespaceService;
    this.provisionService = provisionService;
  }

  @Override
  public void start() throws Exception {
    executorService = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat("default-entry-creator-for-dev-space-env").build());
    executorService.submit(this::run);
  }

  @Override
  public void stop() throws Exception {
    executorService.shutdownNow();
    executorService.awaitTermination(30, TimeUnit.SECONDS);
  }

  public void run() {
    log.info("Creating default provisioning for dev spaces.....");
    try {
      if (env.equals(DEV_SPACE_ENV_TYPE) && !devSpaceDefaultBackstageNamespace.isEmpty()
          && !devSpaceDefaultAccountId.isEmpty()) {
        namespaceService.createDevSpaceEnvDefaultMappingEntry(
            devSpaceDefaultAccountId, devSpaceDefaultBackstageNamespace);
        provisionService.createBackstageBackendSecret(devSpaceDefaultAccountId);
        provisionService.createDefaultPermissions(devSpaceDefaultAccountId);
        provisionService.createBackstageOverrideConfig(devSpaceDefaultAccountId);
      }
    } catch (Exception e) {
      log.error("Default provisioning  is unsuccessful in DevSpace env", e);
    }
  }
}
