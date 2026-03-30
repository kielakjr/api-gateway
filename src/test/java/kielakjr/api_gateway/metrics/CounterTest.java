package kielakjr.api_gateway.metrics;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class CounterTest {

  @Test
  void getCount_initial_returnsZero() {
    Counter counter = new Counter();

    assertEquals(0, counter.getCount());
  }

  @Test
  void increment_once_returnsOne() {
    Counter counter = new Counter();

    counter.increment();

    assertEquals(1, counter.getCount());
  }

  @Test
  void increment_multiple_returnsCorrectCount() {
    Counter counter = new Counter();

    counter.increment();
    counter.increment();
    counter.increment();

    assertEquals(3, counter.getCount());
  }

  @Test
  void increment_concurrent_isThreadSafe() throws InterruptedException {
    Counter counter = new Counter();
    int threadCount = 10;
    int incrementsPerThread = 1000;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        for (int j = 0; j < incrementsPerThread; j++) {
          counter.increment();
        }
        latch.countDown();
      });
    }

    latch.await();
    executor.shutdown();

    assertEquals(threadCount * incrementsPerThread, counter.getCount());
  }
}
