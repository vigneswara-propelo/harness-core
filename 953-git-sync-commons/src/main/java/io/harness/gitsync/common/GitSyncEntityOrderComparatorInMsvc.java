package io.harness.gitsync.common;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;

import java.util.Comparator;

@OwnedBy(PL)
public interface GitSyncEntityOrderComparatorInMsvc {
  Comparator<EntityDetailProtoDTO> comparator();
}
