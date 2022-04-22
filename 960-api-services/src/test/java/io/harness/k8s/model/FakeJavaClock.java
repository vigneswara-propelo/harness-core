package io.harness.k8s.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public class FakeJavaClock extends Clock {
  private final FakeGoogleClock googleClock;

  public FakeJavaClock(Instant now) {
    googleClock = new FakeGoogleClock(now);
  }

  @Override
  public ZoneId getZone() {
    return systemDefaultZone().getZone();
  }

  @Override
  public Clock withZone(ZoneId zone) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public Instant advance(Duration duration) {
    return googleClock.advance(duration);
  }

  public Instant windBack(Duration duration) {
    return googleClock.windBack(duration);
  }

  public FakeGoogleClock getGoogleClock() {
    return googleClock;
  }

  @Override
  public long millis() {
    return googleClock.currentTimeMillis();
  }

  @Override
  public Instant instant() {
    return Instant.ofEpochMilli(millis());
  }
}
