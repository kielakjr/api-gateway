package kielakjr.api_gateway.proxy;

public class ProxyResponse {
  private int statusCode;
  private byte[] body;

  public ProxyResponse(int statusCode, byte[] body) {
    this.statusCode = statusCode;
    this.body = body;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public byte[] getBody() {
    return body;
  }
}
