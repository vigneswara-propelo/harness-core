/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.plugin.services;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.common.PipelineTriggerUtils;
import io.harness.idp.namespace.service.NamespaceService;
import io.harness.idp.plugin.config.CustomPluginsConfig;
import io.harness.spec.server.idp.v1.model.NamespaceInfo;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.IDP)
@Slf4j
public class CustomPluginServiceImpl implements CustomPluginService {
  private static final Retry retry =
      PipelineTriggerUtils.buildRetryAndRegisterListeners(CustomPluginServiceImpl.class.getSimpleName());
  @Inject @Named("customPlugins") CustomPluginsConfig customPluginsConfig;
  @Inject private NamespaceService namespaceService;

  @Override
  public void triggerBuildPipeline(String accountIdentifier) {
    NamespaceInfo namespaceInfo = namespaceService.getNamespaceForAccountIdentifier(accountIdentifier);
    String namespace = namespaceInfo.getNamespace();
    String url = customPluginsConfig.getTriggerPipelineUrl();
    PipelineTriggerUtils.trigger(accountIdentifier, namespace, url, retry);
  }
}
