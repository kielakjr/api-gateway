package kielakjr.api_gateway.config;

import java.util.List;
import kielakjr.api_gateway.loadbalancer.LoadBalancerStrategy;

public class GatewayConfig {
  private ServerConfig server;
  private List<RouteConfig> routes;
  private int rateLimitPerMinute;
  private LoadBalancerStrategy loadBalancerStrategy;
  private ConnectionPoolConfig connectionPool;

  public ServerConfig getServer() {
    return server;
  }

  public void setServer(ServerConfig server) {
    this.server = server;
  }

  public List<RouteConfig> getRoutes() {
    return routes;
  }

  public void setRoutes(List<RouteConfig> routes) {
    this.routes = routes;
  }

  public int getRateLimitPerMinute() {
    return rateLimitPerMinute;
  }

  public void setRateLimitPerMinute(int rateLimitPerMinute) {
    this.rateLimitPerMinute = rateLimitPerMinute;
  }

  public LoadBalancerStrategy getLoadBalancerStrategy() {
    return loadBalancerStrategy;
  }

  public void setLoadBalancerStrategy(LoadBalancerStrategy loadBalancerStrategy) {
    this.loadBalancerStrategy = loadBalancerStrategy;
  }

  public ConnectionPoolConfig getConnectionPool() {
    return connectionPool;
  }

  public void setConnectionPool(ConnectionPoolConfig connectionPool) {
    this.connectionPool = connectionPool;
  }
}
