package kielakjr.api_gateway.metrics;

import java.util.concurrent.atomic.AtomicLong;

public class Counter {
  private AtomicLong count = new AtomicLong(0);

  public void increment() {
    count.incrementAndGet();
  }

  public long getCount() {
    return count.get();
  }
}
