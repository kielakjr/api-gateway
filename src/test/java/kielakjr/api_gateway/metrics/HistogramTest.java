package kielakjr.api_gateway.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HistogramTest {

  @Test
  void getPercentile_empty_returnsZero() {
    Histogram histogram = new Histogram();

    assertEquals(0, histogram.getPercentile(50));
  }

  @Test
  void getPercentile_singleValue_returnsThatValue() {
    Histogram histogram = new Histogram();
    histogram.record(42);

    assertEquals(42, histogram.getPercentile(50));
    assertEquals(42, histogram.getPercentile(99));
  }

  @Test
  void getPercentile_p50_returnsMedian() {
    Histogram histogram = new Histogram();
    for (int i = 1; i <= 100; i++) {
      histogram.record(i);
    }

    double p50 = histogram.getPercentile(50);
    assertEquals(50, p50);
  }

  @Test
  void getPercentile_p95_returnsCorrectValue() {
    Histogram histogram = new Histogram();
    for (int i = 1; i <= 100; i++) {
      histogram.record(i);
    }

    double p95 = histogram.getPercentile(95);
    assertEquals(95, p95);
  }

  @Test
  void getPercentile_p99_returnsCorrectValue() {
    Histogram histogram = new Histogram();
    for (int i = 1; i <= 100; i++) {
      histogram.record(i);
    }

    double p99 = histogram.getPercentile(99);
    assertEquals(99, p99);
  }

  @Test
  void record_multipleValues_allStored() {
    Histogram histogram = new Histogram();
    histogram.record(10);
    histogram.record(20);
    histogram.record(30);

    double p50 = histogram.getPercentile(50);
    assertEquals(20, p50);
  }

  @Test
  void getPercentile_unsortedInput_sortsCorrectly() {
    Histogram histogram = new Histogram();
    histogram.record(100);
    histogram.record(1);
    histogram.record(50);
    histogram.record(25);
    histogram.record(75);

    double p50 = histogram.getPercentile(50);
    assertEquals(50, p50);
  }

  @Test
  void getPercentile_duplicateValues_handlesCorrectly() {
    Histogram histogram = new Histogram();
    histogram.record(5);
    histogram.record(5);
    histogram.record(5);
    histogram.record(5);

    assertEquals(5, histogram.getPercentile(50));
    assertEquals(5, histogram.getPercentile(99));
  }

  @Test
  void getPercentile_twoValues_p50ReturnsFirst() {
    Histogram histogram = new Histogram();
    histogram.record(10);
    histogram.record(20);

    double p50 = histogram.getPercentile(50);
    assertEquals(10, p50);
  }
}
