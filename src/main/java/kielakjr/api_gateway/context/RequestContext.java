package kielakjr.api_gateway.context;

import java.util.UUID;

public class RequestContext {
  private String requestId;
  private String clientIp;
  private long startTimeNanos;
  private String resolvedUpstream;
  private String matchedRoute;

  public RequestContext(String clientIp) {
    this.requestId = UUID.randomUUID().toString();
    this.clientIp = clientIp;
    this.startTimeNanos = System.nanoTime();
  }

  public String getRequestId() {
    return requestId;
  }

  public String getClientIp() {
    return clientIp;
  }

  public long getStartTimeNanos() {
    return startTimeNanos;
  }

  public String getResolvedUpstream() {
    return resolvedUpstream;
  }

  public void setResolvedUpstream(String resolvedUpstream) {
    this.resolvedUpstream = resolvedUpstream;
  }

  public String getMatchedRoute() {
    return matchedRoute;
  }

  public void setMatchedRoute(String matchedRoute) {
    this.matchedRoute = matchedRoute;
  }

}
