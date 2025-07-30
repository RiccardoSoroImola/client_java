package io.prometheus.metrics.tracer.initializer;

import io.prometheus.metrics.tracer.agent.OpenTelemetryAgentSpanContext;
import io.prometheus.metrics.tracer.common.SpanContext;
import io.prometheus.metrics.tracer.otel.OpenTelemetrySpanContext;
import java.util.concurrent.atomic.AtomicReference;

public class SpanContextSupplier {

  private SpanContextSupplier() {
    throw new IllegalStateException("Utility class");
  }

  private static final AtomicReference<SpanContext> spanContextRef =
      new AtomicReference<SpanContext>();

  public static void setSpanContext(SpanContext spanContext) {
    spanContextRef.set(spanContext);
  }

  public static boolean hasSpanContext() {
    return getSpanContext() != null;
  }

  public static SpanContext getSpanContext() {
    return spanContextRef.get();
  }

  static {
    try {
      if (OpenTelemetrySpanContext.isAvailable()) {
        spanContextRef.set(new OpenTelemetrySpanContext());
      }
    } catch (NoClassDefFoundError | UnsupportedClassVersionError ignored) {
      // tracer_otel dependency not found
    }
    try {
      if (OpenTelemetryAgentSpanContext.isAvailable()) {
        spanContextRef.set(new OpenTelemetryAgentSpanContext());
      }
    } catch (NoClassDefFoundError | UnsupportedClassVersionError ignored) {
      // tracer_otel_agent dependency not found
    }
  }
}
