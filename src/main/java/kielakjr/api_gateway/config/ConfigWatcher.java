package kielakjr.api_gateway.config;

import java.nio.file.FileSystems;
import java.nio.file.WatchService;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import kielakjr.api_gateway.router.Router;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;



public class ConfigWatcher {
  private final String configPath;
  private AtomicReference<Router> routerRef;
  private final ConfigLoader configLoader;
  private final Logger log = LoggerFactory.getLogger(ConfigWatcher.class);

  public ConfigWatcher(String configPath, AtomicReference<Router> routerRef) {
    this.configPath = configPath;
    this.configLoader = new ConfigLoader();
    this.routerRef = routerRef;
  }

  public void start() throws IOException {
    WatchService watchService = FileSystems.getDefault().newWatchService();
    Path dir = Paths.get(configPath).toAbsolutePath().getParent();
    dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

    Thread thread = new Thread(() -> {
      while (true) {
        try {
          WatchKey key = watchService.take();
          for (var event : key.pollEvents()) {
            if (event.context().toString().equals(Path.of(configPath).getFileName().toString())) {
              try {
                Router newRouter = new Router(configLoader.loadConfig(configPath).getRoutes(), configLoader.loadConfig(configPath).getLoadBalancerStrategy());
                routerRef.set(newRouter);
                log.info("Configuration reloaded successfully");
              } catch (Exception e) {
                log.error("Failed to reload configuration: " + e.getMessage());
              }
            }
          }
          key.reset();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    });
    thread.setDaemon(true);
    thread.start();
  }
}
