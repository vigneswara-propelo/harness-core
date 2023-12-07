/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.usage.utils;

import static io.harness.licensing.usage.beans.cd.CDLicenseUsageConstants.SERVICE_INSTANCE_LIMIT;
import static io.harness.licensing.usage.beans.cd.CDLicenseUsageConstants.SERVICE_INSTANCE_LIMIT_LAMBDA;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.InstanceType;
import io.harness.exception.InvalidArgumentsException;

import java.time.Instant;
import java.time.Period;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class LicenseUsageUtils {
  private final Set<InstanceType> lambdaInstanceTypes =
      Set.of(InstanceType.AWS_LAMBDA_INSTANCE, InstanceType.SERVERLESS_AWS_LAMBDA_INSTANCE,
          InstanceType.AWS_SAM_INSTANCE, InstanceType.GOOGLE_CLOUD_FUNCTIONS_INSTANCE);

  public static long computeLicenseConsumed(long serviceInstanceCount, InstanceType instanceType) {
    final int instanceLimit;

    if (instanceType == null && serviceInstanceCount != 0) {
      throw new InvalidArgumentsException("instanceType cannot be null for non-zero serviceInstanceCount");
    }

    if (instanceType == null && serviceInstanceCount == 0) {
      // instanceType will be null when service deployment is attempted,
      // but there is no entry in ng_instance_stats. ie serviceInstanceCount is 0.
      // We consume 1 active service license in this case for both lambda and non lambda deployments.
      return 1;
    }

    if (lambdaInstanceTypes.contains(instanceType)) {
      instanceLimit = SERVICE_INSTANCE_LIMIT_LAMBDA;
    } else {
      instanceLimit = SERVICE_INSTANCE_LIMIT;
    }

    if (serviceInstanceCount <= instanceLimit) {
      return 1;
    } else {
      return ((serviceInstanceCount - 1) / instanceLimit) + 1;
    }
  }

  public static long getEpochMilliNDaysAgo(long timestamp, int days) {
    return Instant.ofEpochMilli(timestamp).minus(Period.ofDays(days)).toEpochMilli();
  }
}
