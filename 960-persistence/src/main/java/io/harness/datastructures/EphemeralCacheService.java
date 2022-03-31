package io.harness.datastructures;

import java.util.Set;

public interface EphemeralCacheService {
  <V> Set<V> getDistributedSet(String setName);
}
