package kielakjr.api_gateway.proxy;

public class ProxyResponse {
  private int statusCode;
  private String contentType;
  private byte[] body;

  public ProxyResponse(int statusCode, byte[] body, String contentType) {
    this.statusCode = statusCode;
    this.body = body;
    this.contentType = contentType;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getContentType() {
    return contentType;
  }

  public byte[] getBody() {
    return body;
  }
}
