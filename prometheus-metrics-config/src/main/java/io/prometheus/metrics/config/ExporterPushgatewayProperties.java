package io.prometheus.metrics.config;

import java.util.Map;

public class ExporterPushgatewayProperties {

  private static final String ADDRESS = "address";
  private static final String JOB = "job";
  private static final String SCHEME = "scheme";
  private static final String PREFIX = "io.prometheus.exporter.pushgateway";
  private final String schemeField;
  private final String addressField;
  private final String jobField;

  private ExporterPushgatewayProperties(String address, String job, String scheme) {
    this.addressField = address;
    this.jobField = job;
    this.schemeField = scheme;
  }

  /** Address of the Pushgateway in the form {@code host:port}. Default is {@code localhost:9091} */
  public String getAddress() {
    return addressField;
  }

  /**
   * {@code job} label for metrics being pushed. Default is the name of the JAR file that is
   * running.
   */
  public String getJob() {
    return jobField;
  }

  /**
   * Scheme to be used when pushing metrics to the pushgateway. Must be "http" or "https". Default
   * is "http".
   */
  public String getScheme() {
    return schemeField;
  }

  /**
   * Note that this will remove entries from {@code properties}. This is because we want to know if
   * there are unused properties remaining after all properties have been loaded.
   */
  static ExporterPushgatewayProperties load(Map<Object, Object> properties)
      throws PrometheusPropertiesException {
    String address = Util.loadString(PREFIX + "." + ADDRESS, properties);
    String job = Util.loadString(PREFIX + "." + JOB, properties);
    String scheme = Util.loadString(PREFIX + "." + SCHEME, properties);
    if (scheme != null) {
      if (!scheme.equals("http") && !scheme.equals("https")) {
        throw new PrometheusPropertiesException(
            String.format(
                "%s.%s: Illegal value. Expecting 'http' or 'https'. Found: %s",
                PREFIX, SCHEME, scheme));
      }
    }
    return new ExporterPushgatewayProperties(address, job, scheme);
  }
}
