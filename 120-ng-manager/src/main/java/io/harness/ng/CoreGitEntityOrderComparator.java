package io.harness.ng;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper.mapEventToRestEntityType;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.gitsync.common.GitSyncEntityOrderComparatorInMsvc;

import com.google.common.collect.Lists;
import java.util.Comparator;
import java.util.List;

@OwnedBy(PL)
public class CoreGitEntityOrderComparator implements GitSyncEntityOrderComparatorInMsvc {
  public static final List<EntityType> sortOrder = Lists.newArrayList(EntityType.CONNECTORS);

  @Override
  public Comparator<EntityDetailProtoDTO> comparator() {
    return new Comparator<EntityDetailProtoDTO>() {
      @Override
      public int compare(final EntityDetailProtoDTO o1, final EntityDetailProtoDTO o2) {
        EntityType entityType1 = mapEventToRestEntityType(o1.getType());
        EntityType entityType2 = mapEventToRestEntityType(o2.getType());
        return Integer.compare(sortOrder.indexOf(entityType1), sortOrder.indexOf(entityType2));
      }
    };
  }
}
