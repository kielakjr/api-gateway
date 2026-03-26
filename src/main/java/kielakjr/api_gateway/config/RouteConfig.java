package kielakjr.api_gateway.config;

import java.util.List;

public class RouteConfig {
  private String path;
  private List<String> upstreams;

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public List<String> getUpstreams() {
    return upstreams;
  }

  public void setUpstreams(List<String> upstreams) {
    this.upstreams = upstreams;
  }
}
