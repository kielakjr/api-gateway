package kielakjr.api_gateway.config;

import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.databind.ObjectMapper;
import java.io.File;

public class ConfigLoader {
  ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

  public GatewayConfig loadConfig(String filePath) throws Exception {
    return objectMapper.readValue(new File(filePath), GatewayConfig.class);
  }
}
