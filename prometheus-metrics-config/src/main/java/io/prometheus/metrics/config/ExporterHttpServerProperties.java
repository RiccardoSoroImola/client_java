package io.prometheus.metrics.config;

import java.util.Map;

/** Properties starting with io.prometheus.exporter.httpServer */
public class ExporterHttpServerProperties {

  private static final String PORT = "port";
  private static final String PREFIX = "io.prometheus.exporter.httpServer";
  private final Integer portValue;

  private ExporterHttpServerProperties(Integer portValue) {
    this.portValue = portValue;
  }

  public Integer getPort() {
    return portValue;
  }

  /**
   * Note that this will remove entries from {@code properties}. This is because we want to know if
   * there are unused properties remaining after all properties have been loaded.
   */
  static ExporterHttpServerProperties load(Map<Object, Object> properties)
      throws PrometheusPropertiesException {
    Integer portValue = Util.loadInteger(PREFIX + "." + PORT, properties);
    Util.assertValue(portValue, t -> t > 0, "Expecting value > 0.", PREFIX, PORT);
    return new ExporterHttpServerProperties(portValue);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private Integer portValue;

    private Builder() {}

    public Builder port(int port) {
      this.portValue = port;
      return this;
    }

    public ExporterHttpServerProperties build() {
      return new ExporterHttpServerProperties(portValue);
    }
  }
}
