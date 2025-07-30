package io.prometheus.metrics.config;

import java.util.HashMap;
import java.util.Map;

public class ExporterOpenTelemetryProperties {

  // See
  // https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md
  private static final String PROTOCOL = "protocol"; // otel.exporter.otlp.protocol
  private static final String ENDPOINT = "endpoint"; // otel.exporter.otlp.endpoint
  private static final String HEADERS = "headers"; // otel.exporter.otlp.headers
  private static final String INTERVAL_SECONDS = "intervalSeconds"; // otel.metric.export.interval
  private static final String TIMEOUT_SECONDS = "timeoutSeconds"; // otel.exporter.otlp.timeout
  private static final String SERVICE_NAME = "serviceName"; // otel.service.name
  private static final String SERVICE_NAMESPACE = "serviceNamespace";
  private static final String SERVICE_INSTANCE_ID = "serviceInstanceId";
  private static final String SERVICE_VERSION = "serviceVersion";
  private static final String RESOURCE_ATTRIBUTES =
      "resourceAttributes"; // otel.resource.attributes
  private static final String PREFIX = "io.prometheus.exporter.opentelemetry";

  private final String protocolValue;
  private final String endpointValue;
  private final Map<String, String> headersMap;
  private final String intervalValue;
  private final String timeoutValue;
  private final String serviceNameValue;
  private final String serviceNamespaceValue;
  private final String serviceInstanceIdValue;
  private final String serviceVersionValue;
  private final Map<String, String> resourceAttributesMap;

  private ExporterOpenTelemetryProperties(
      String protocol,
      String endpoint,
      Map<String, String> headers,
      String interval,
      String timeout,
      String serviceName,
      String serviceNamespace,
      String serviceInstanceId,
      String serviceVersion,
      Map<String, String> resourceAttributes) {
    this.protocolValue = protocol;
    this.endpointValue = endpoint;
    this.headersMap = headers;
    this.intervalValue = interval;
    this.timeoutValue = timeout;
    this.serviceNameValue = serviceName;
    this.serviceNamespaceValue = serviceNamespace;
    this.serviceInstanceIdValue = serviceInstanceId;
    this.serviceVersionValue = serviceVersion;
    this.resourceAttributesMap = resourceAttributes;
  }

  public String getProtocol() {
    return protocolValue;
  }

  public String getEndpoint() {
    return endpointValue;
  }

  public Map<String, String> getHeaders() {
    return headersMap;
  }

  public String getInterval() {
    return intervalValue;
  }

  public String getTimeout() {
    return timeoutValue;
  }

  public String getServiceName() {
    return serviceNameValue;
  }

  public String getServiceNamespace() {
    return serviceNamespaceValue;
  }

  public String getServiceInstanceId() {
    return serviceInstanceIdValue;
  }

  public String getServiceVersion() {
    return serviceVersionValue;
  }

  public Map<String, String> getResourceAttributes() {
    return resourceAttributesMap;
  }

  /**
   * Note that this will remove entries from {@code properties}. This is because we want to know if
   * there are unused properties remaining after all properties have been loaded.
   */
  static ExporterOpenTelemetryProperties load(Map<Object, Object> properties)
      throws PrometheusPropertiesException {
    String protocol = Util.loadString(PREFIX + "." + PROTOCOL, properties);
    String endpoint = Util.loadString(PREFIX + "." + ENDPOINT, properties);
    Map<String, String> headers = Util.loadMap(PREFIX + "." + HEADERS, properties);
    String interval = Util.loadStringAddSuffix(PREFIX + "." + INTERVAL_SECONDS, properties, "s");
    String timeout = Util.loadStringAddSuffix(PREFIX + "." + TIMEOUT_SECONDS, properties, "s");
    String serviceName = Util.loadString(PREFIX + "." + SERVICE_NAME, properties);
    String serviceNamespace = Util.loadString(PREFIX + "." + SERVICE_NAMESPACE, properties);
    String serviceInstanceId = Util.loadString(PREFIX + "." + SERVICE_INSTANCE_ID, properties);
    String serviceVersion = Util.loadString(PREFIX + "." + SERVICE_VERSION, properties);
    Map<String, String> resourceAttributes =
        Util.loadMap(PREFIX + "." + RESOURCE_ATTRIBUTES, properties);
    return new ExporterOpenTelemetryProperties(
        protocol,
        endpoint,
        headers,
        interval,
        timeout,
        serviceName,
        serviceNamespace,
        serviceInstanceId,
        serviceVersion,
        resourceAttributes);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String protocolValue;
    private String endpointValue;
    private final Map<String, String> headersMap = new HashMap<>();
    private String intervalValue;
    private String timeoutValue;
    private String serviceNameValue;
    private String serviceNamespaceValue;
    private String serviceInstanceIdValue;
    private String serviceVersionValue;
    private final Map<String, String> resourceAttributesMap = new HashMap<>();

    private Builder() {}

    public Builder protocol(String protocol) {
      if (!protocol.equals("grpc") && !protocol.equals("http/protobuf")) {
        throw new IllegalArgumentException(
            protocol + ": Unsupported protocol. Expecting grpc or http/protobuf");
      }
      this.protocolValue = protocol;
      return this;
    }

    public Builder endpoint(String endpoint) {
      this.endpointValue = endpoint;
      return this;
    }

    /** Add a request header. Call multiple times to add multiple headers. */
    public Builder header(String name, String value) {
      this.headersMap.put(name, value);
      return this;
    }

    public Builder intervalSeconds(int intervalSeconds) {
      if (intervalSeconds <= 0) {
        throw new IllegalArgumentException(intervalSeconds + ": Expecting intervalSeconds > 0");
      }
      this.intervalValue = intervalSeconds + "s";
      return this;
    }

    public Builder timeoutSeconds(int timeoutSeconds) {
      if (timeoutSeconds <= 0) {
        throw new IllegalArgumentException(timeoutSeconds + ": Expecting timeoutSeconds > 0");
      }
      this.timeoutValue = timeoutSeconds + "s";
      return this;
    }

    public Builder serviceName(String serviceName) {
      this.serviceNameValue = serviceName;
      return this;
    }

    public Builder serviceNamespace(String serviceNamespace) {
      this.serviceNamespaceValue = serviceNamespace;
      return this;
    }

    public Builder serviceInstanceId(String serviceInstanceId) {
      this.serviceInstanceIdValue = serviceInstanceId;
      return this;
    }

    public Builder serviceVersion(String serviceVersion) {
      this.serviceVersionValue = serviceVersion;
      return this;
    }

    public Builder resourceAttribute(String name, String value) {
      this.resourceAttributesMap.put(name, value);
      return this;
    }

    public ExporterOpenTelemetryProperties build() {
      return new ExporterOpenTelemetryProperties(
          protocolValue,
          endpointValue,
          headersMap,
          intervalValue,
          timeoutValue,
          serviceNameValue,
          serviceNamespaceValue,
          serviceInstanceIdValue,
          serviceVersionValue,
          resourceAttributesMap);
    }
  }
}
