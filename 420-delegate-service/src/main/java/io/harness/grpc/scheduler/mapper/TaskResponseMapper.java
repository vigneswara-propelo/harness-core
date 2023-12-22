/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.grpc.scheduler.mapper;

import io.harness.delegate.GetTaskStatusResponse;
import io.harness.mapstruct.protobuf.ProtobufMapperConfig;
import io.harness.mapstruct.protobuf.StandardProtobufMappers;
import io.harness.taskresponse.TaskResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {StandardProtobufMappers.class}, config = ProtobufMapperConfig.class)
public interface TaskResponseMapper {
  TaskResponseMapper INSTANCE = Mappers.getMapper(TaskResponseMapper.class);

  @Mapping(source = "uuid", target = "taskId.id")
  @Mapping(source = "code", target = "status")
  GetTaskStatusResponse toProto(TaskResponse compute);
}
