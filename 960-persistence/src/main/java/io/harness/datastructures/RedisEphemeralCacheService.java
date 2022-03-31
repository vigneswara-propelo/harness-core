package io.harness.datastructures;

import java.util.Set;
import org.redisson.api.RedissonClient;

public class RedisEphemeralCacheService implements EphemeralCacheService {
  RedissonClient redissonClient;

  RedisEphemeralCacheService(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }

  @Override
  public <V> Set<V> getDistributedSet(String setName) {
    return new RedisHSet<>(redissonClient, setName);
  }
}
