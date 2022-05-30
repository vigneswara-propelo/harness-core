package io.harness.steps.matrix;

import io.harness.pms.contracts.advisers.AdviserObtainment;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StrategyMetadata {
  String childNodeId;
  String strategyNodeId;
  List<AdviserObtainment> adviserObtainments;
}
