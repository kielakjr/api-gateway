package kielakjr.api_gateway.config;

public class TimeoutsConfig {
  private int readSeconds;
  private int writeSeconds;

  public int getReadSeconds() {
    return readSeconds;
  }

  public void setReadSeconds(int readSeconds) {
    this.readSeconds = readSeconds;
  }

  public int getWriteSeconds() {
    return writeSeconds;
  }

  public void setWriteSeconds(int writeSeconds) {
    this.writeSeconds = writeSeconds;
  }
}
