/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.config;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
@OwnedBy(HarnessTeam.CE)
public class GcpSyncSmpConfig {
  private String nextgenCeSecretName;
  private String batchProcessingSecretName;
  private String k8sJobContainerName;
  private String k8sJobName;
  private String k8sJobPythonImage;
  private String batchProcessingConfigMapName;
  private String hmacAccessKey;
  private String hmacSecretKey;
  private String serviceAccountCredentialKey;
  private String batchProcessingMountSecretName;
  private String clickHouseSecretName;
  private String clickHousePasswordKey;
}
