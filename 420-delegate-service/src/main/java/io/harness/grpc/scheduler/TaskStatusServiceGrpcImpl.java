/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.grpc.scheduler;

import io.harness.delegate.GetTaskStatusRequest;
import io.harness.delegate.TaskStatusServiceGrpc.TaskStatusServiceImplBase;
import io.harness.delegate.getTaskStatusResponse;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class TaskStatusServiceGrpcImpl extends TaskStatusServiceImplBase {
  @Override
  public void getTaskStatus(
      final GetTaskStatusRequest request, final StreamObserver<getTaskStatusResponse> responseObserver) {
    responseObserver.onError(Status.UNIMPLEMENTED.withDescription("Operation not implemented").asRuntimeException());
  }
}
