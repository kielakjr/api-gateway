package kielakjr.api_gateway.metrics;

import java.util.concurrent.CopyOnWriteArrayList;

public class Histogram {
  private CopyOnWriteArrayList<Long> values = new CopyOnWriteArrayList<>();

  public void record(long value) {
    values.add(value);
  }

  public double getPercentile(double percentile) {
    if (values.isEmpty()) {
      return 0;
    }
    values.sort(Long::compare);
    int index = (int) Math.ceil(percentile / 100.0 * values.size()) - 1;
    return values.get(index);
  }
}
