package io.prometheus.metrics.instrumentation.jvm;

import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.metrics.config.PrometheusProperties;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.lang.management.ManagementFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JvmMetricsTest {

  @BeforeEach
  void setUp() {
    PrometheusRegistry.defaultRegistry.clear();
  }

  @Test
  void testRegisterIdempotent() {
    PrometheusRegistry registry = new PrometheusRegistry();
    assertThat(registry.scrape().size()).isZero();
    JvmMetrics.builder().register(registry);
    int afterFirst = registry.scrape().size();
    assertThat(afterFirst).isGreaterThan(0);
    JvmMetrics.builder().register(registry);
    assertThat(registry.scrape().size()).isEqualTo(afterFirst);
  }

  @Test
  void pool() {
    // for coverage
    JvmMemoryPoolAllocationMetrics.builder(PrometheusProperties.get())
        .withGarbageCollectorBeans(ManagementFactory.getGarbageCollectorMXBeans())
        .register();
    assertThat(PrometheusRegistry.defaultRegistry.scrape().size()).isGreaterThan(0);
  }

  @Test
  void testJvmMetrics() {
    JvmMetrics.builder(PrometheusProperties.get()).register();
    int afterFirst = PrometheusRegistry.defaultRegistry.scrape().size();
    assertThat(afterFirst).isGreaterThan(0);
    JvmMetrics.builder().register();
    assertThat(PrometheusRegistry.defaultRegistry.scrape().size()).isEqualTo(afterFirst);
  }
}
