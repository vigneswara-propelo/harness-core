/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.morphia;

import io.harness.beans.SecretManagerConfig;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.BaseVaultConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.InstanaConfig;
import software.wings.beans.JiraConfig;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.PcfConfig;
import software.wings.beans.SSHVaultConfig;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.SftpConfig;
import software.wings.beans.SmbConfig;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SumoConfig;
import software.wings.beans.appmanifest.HelmChart;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig;
import software.wings.delegatetasks.cv.beans.CustomLogResponseMapper;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.impl.analysis.CustomLogDataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.logz.LogzDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.sumo.SumoDataCollectionInfo;

import java.util.Set;

public class DelegateTasksMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(NewRelicMetricDataRecord.class);
    set.add(DelegateConnectionResult.class);
    set.add(HelmChart.class);
    set.add(BaseVaultConfig.class);
    set.add(SecretManagerConfig.class);
    set.add(SSHVaultConfig.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    w.put("service.impl.analysis.DataCollectionTaskResult", DataCollectionTaskResult.class);
    w.put("service.impl.analysis.CustomLogDataCollectionInfo", CustomLogDataCollectionInfo.class);
    w.put("delegatetasks.cv.beans.CustomLogResponseMapper", CustomLogResponseMapper.class);
    w.put("beans.AppDynamicsConfig", AppDynamicsConfig.class);
    w.put("beans.NewRelicConfig", NewRelicConfig.class);
    w.put("beans.DynaTraceConfig", DynaTraceConfig.class);
    w.put("beans.SumoConfig", SumoConfig.class);
    w.put("service.impl.sumo.SumoDataCollectionInfo", SumoDataCollectionInfo.class);
    w.put("beans.config.LogzConfig", LogzConfig.class);
    w.put("beans.ElkConfig", ElkConfig.class);
    w.put("service.impl.elk.ElkDataCollectionInfo", ElkDataCollectionInfo.class);
    w.put("service.impl.logz.LogzDataCollectionInfo", LogzDataCollectionInfo.class);
    w.put("beans.EcrConfig", EcrConfig.class);
    w.put("beans.SecretManagerConfig", SecretManagerConfig.class);
    w.put("beans.SSHVaultConfig", SSHVaultConfig.class);
    w.put("beans.BaseVaultConfig", BaseVaultConfig.class);
    w.put("beans.GcpConfig", GcpConfig.class);
    w.put("beans.artifact.ArtifactFile", ArtifactFile.class);
    w.put("beans.ServiceNowConfig", ServiceNowConfig.class);
    w.put("beans.DockerConfig", DockerConfig.class);
    w.put("beans.config.NexusConfig", NexusConfig.class);
    w.put("beans.JiraConfig", JiraConfig.class);
    w.put("beans.SplunkConfig", SplunkConfig.class);
    w.put("beans.settings.azureartifacts.AzureArtifactsPATConfig", AzureArtifactsPATConfig.class);
    w.put("beans.SftpConfig", SftpConfig.class);
    w.put("beans.InstanaConfig", InstanaConfig.class);
    w.put("beans.HostConnectionAttributes", HostConnectionAttributes.class);
    w.put("beans.PcfConfig", PcfConfig.class);
    w.put("beans.AzureConfig", AzureConfig.class);
    w.put("beans.BambooConfig", BambooConfig.class);
    w.put("beans.SmbConfig", SmbConfig.class);
    w.put("beans.config.ArtifactoryConfig", ArtifactoryConfig.class);
    w.put("helpers.ext.mail.SmtpConfig", SmtpConfig.class);
  }
}
