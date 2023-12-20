/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.grpc;

import io.harness.delegate.TaskStatusServiceGrpc;
import io.harness.govern.ProviderModule;
import io.harness.grpc.auth.ServiceAuthCallCredentials;
import io.harness.security.ServiceTokenGenerator;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DelegateSchedulingGrpcClient extends ProviderModule {
  private final String serviceSecret;

  @Provides
  @Singleton
  public TaskStatusServiceGrpc.TaskStatusServiceBlockingStub scheduleTaskServiceBlockingStub(
      @Named("delegate-service-channel") Channel channel,
      @Named("ts-call-credentials") CallCredentials callCredentials) {
    return TaskStatusServiceGrpc.newBlockingStub(channel).withCallCredentials(callCredentials);
  }

  @Named("ts-call-credentials")
  @Provides
  @Singleton
  public CallCredentials tsCallCredentials() {
    return new ServiceAuthCallCredentials(serviceSecret, new ServiceTokenGenerator(), "task-status-service");
  }
}
