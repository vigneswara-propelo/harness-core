package io.harness.ccm.views.businessMapping.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
@OwnedBy(CE)
public class UnallocatedCost {
  UnallocatedCostStrategy strategy;

  // Will be present UnallocatedCostStrategy is DISPLAY_NAME
  String label;

  // Will be present UnallocatedCostStrategy is SHARE
  SharingStrategy sharingStrategy;
  List<SharedCostSplit> splits;
}
