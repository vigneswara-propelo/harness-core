package io.harness.cdng.pipeline.executions.beans;

import io.harness.cdng.pipeline.executions.beans.dto.PipelineExecutionDTO;
import io.harness.executions.beans.ExecutionGraph;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PipelineExecutionDetail {
  PipelineExecutionDTO pipelineExecution;
  ExecutionGraph stageGraph;
}
