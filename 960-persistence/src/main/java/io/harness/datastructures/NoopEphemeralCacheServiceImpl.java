package io.harness.datastructures;

import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class NoopEphemeralCacheServiceImpl implements EphemeralCacheService {
  @Override
  public <V> Set<V> getDistributedSet(String setName) {
    return new HashSet<>();
  }
}
