package kielakjr.api_gateway.config;

public class ConnectionPoolConfig {
  private int connectTimeoutSeconds;
  private int requestTimeoutSeconds;

  public int getConnectTimeoutSeconds() {
    return connectTimeoutSeconds;
  }

  public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
    this.connectTimeoutSeconds = connectTimeoutSeconds;
  }

  public int getRequestTimeoutSeconds() {
    return requestTimeoutSeconds;
  }

  public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
    this.requestTimeoutSeconds = requestTimeoutSeconds;
  }
}
