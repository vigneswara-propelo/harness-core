package io.harness.state.execution.status;

public enum NodeExecutionStatus {

  // In Progress statuses : All the in progress statuses named with ing in the end
  RUNNING,
  TASK_WAITING,

  // Final Statuses : All the final statuses named with ed in the end
  QUEUED,
  SKIPPED,
  ERRORED,
  FAILED,
  SUCCEEDED

}
