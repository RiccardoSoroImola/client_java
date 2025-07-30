package io.prometheus.metrics.exporter.opentelemetry;

import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import java.util.Properties;

class PrometheusInstrumentationScope {

  private static final String INSTRUMENTATION_SCOPE_PROPERTIES_FILE =
      "instrumentationScope.properties";
  private static final String INSTRUMENTATION_SCOPE_NAME_KEY = "instrumentationScope.name";
  private static final String INSTRUMENTATION_SCOPE_VERSION_KEY = "instrumentationScope.version";

  private PrometheusInstrumentationScope() {
    throw new IllegalStateException("Utility class");
  }

  public static InstrumentationScopeInfo loadInstrumentationScopeInfo() {
    return loadInstrumentationScopeInfo(
        INSTRUMENTATION_SCOPE_PROPERTIES_FILE,
        INSTRUMENTATION_SCOPE_NAME_KEY,
        INSTRUMENTATION_SCOPE_VERSION_KEY);
  }

  static InstrumentationScopeInfo loadInstrumentationScopeInfo(
      String path, String nameKey, String versionKey) {
    try {
      Properties properties = new Properties();
      properties.load(
          PrometheusInstrumentationScope.class.getClassLoader().getResourceAsStream(path));
      String instrumentationScopeName = properties.getProperty(nameKey);
      if (instrumentationScopeName == null) {
        throw new IllegalStateException(
            "Prometheus metrics library initialization error: "
                + nameKey
                + " not found in "
                + path
                + " in classpath.");
      }
      String instrumentationScopeVersion = properties.getProperty(versionKey);
      if (instrumentationScopeVersion == null) {
        throw new IllegalStateException(
            "Prometheus metrics library initialization error: "
                + versionKey
                + " not found in "
                + path
                + " in classpath.");
      }
      return InstrumentationScopeInfo.builder(instrumentationScopeName)
          .setVersion(instrumentationScopeVersion)
          .build();
    } catch (Exception e) {
      throw new IllegalStateException(
          "Prometheus metrics library initialization error: Failed to read "
              + path
              + " from classpath.",
          e);
    }
  }
}
