package io.prometheus.metrics.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

class MetricsPropertiesTest {
  @Test
  void builder() {
    assertThat(MetricsProperties.builder().exemplarsEnabled(true).build().getExemplarsEnabled())
        .isTrue();
    assertThat(
            MetricsProperties.builder().histogramNativeOnly(true).build().getHistogramNativeOnly())
        .isTrue();
    assertThat(
            MetricsProperties.builder()
                .histogramClassicOnly(true)
                .build()
                .getHistogramClassicOnly())
        .isTrue();
    assertThat(
            MetricsProperties.builder()
                .histogramClassicUpperBounds(1.0, 2.0)
                .build()
                .getHistogramClassicUpperBounds())
        .containsExactly(1.0, 2.0);

    assertThat(MetricsProperties.builder().summaryQuantiles(0.1, 0.2).build().getSummaryQuantiles())
        .containsExactly(0.1, 0.2);
    assertThat(
            MetricsProperties.builder().summaryMaxAgeSeconds(1L).build().getSummaryMaxAgeSeconds())
        .isOne();
    assertThat(
            MetricsProperties.builder()
                .summaryQuantiles(0.2)
                .summaryQuantileErrors(1.0)
                .build()
                .getSummaryQuantileErrors())
        .containsExactly(1.0);
    assertThat(
            MetricsProperties.builder()
                .summaryNumberOfAgeBuckets(1)
                .build()
                .getSummaryNumberOfAgeBuckets())
        .isOne();

    MetricsProperties.Builder builder = MetricsProperties.builder();
    builder.summaryNumberOfAgeBuckets(0);
    MetricsProperties.Builder builderWithInvalidValue = builder;
    assertThatExceptionOfType(PrometheusPropertiesException.class)
        .isThrownBy(builderWithInvalidValue::build)
        .withMessage(".summaryNumberOfAgeBuckets: Expecting value > 0. Found: 0");

    MetricsProperties.Builder builder1 = MetricsProperties.builder();
    builder1.summaryQuantiles(2L);
    MetricsProperties.Builder builder1WithInvalidValue = builder1;
    assertThatExceptionOfType(PrometheusPropertiesException.class)
        .isThrownBy(builder1WithInvalidValue::build)
        .withMessage(".summaryQuantiles: Expecting 0.0 <= quantile <= 1.0. Found: 2.0");

    MetricsProperties.Builder builder2 = MetricsProperties.builder();
    builder2.summaryQuantileErrors(0.9);
    MetricsProperties.Builder builder2WithInvalidValue = builder2;
    assertThatExceptionOfType(PrometheusPropertiesException.class)
        .isThrownBy(builder2WithInvalidValue::build)
        .withMessage(
            ".summaryQuantileErrors: Can't configure summaryQuantileErrors without configuring"
                + " summaryQuantiles");

    MetricsProperties.Builder builder3 = MetricsProperties.builder();
    builder3.summaryQuantiles(0.1);
    builder3.summaryQuantileErrors(0.1, 0.9);
    MetricsProperties.Builder builder3WithInvalidValue = builder3;
    assertThatExceptionOfType(PrometheusPropertiesException.class)
        .isThrownBy(builder3WithInvalidValue::build)
        .withMessage(".summaryQuantileErrors: must have the same length as summaryQuantiles");

    MetricsProperties.Builder builder4 = MetricsProperties.builder();
    builder4.summaryQuantiles(0.1);
    builder4.summaryQuantileErrors(-0.9);
    MetricsProperties.Builder builder4WithInvalidValue = builder4;
    assertThatExceptionOfType(PrometheusPropertiesException.class)
        .isThrownBy(builder4WithInvalidValue::build)
        .withMessage(".summaryQuantileErrors: Expecting 0.0 <= error <= 1.0");
  }

  @Test
  void nativeBuilder() {
    assertThat(
            MetricsProperties.builder()
                .histogramNativeInitialSchema(1)
                .build()
                .getHistogramNativeInitialSchema())
        .isOne();
    assertThat(
            MetricsProperties.builder()
                .histogramNativeMinZeroThreshold(.1)
                .build()
                .getHistogramNativeMinZeroThreshold())
        .isEqualTo(.1);
    assertThat(
            MetricsProperties.builder()
                .histogramNativeMaxZeroThreshold(.1)
                .build()
                .getHistogramNativeMaxZeroThreshold())
        .isEqualTo(.1);
    assertThat(
            MetricsProperties.builder()
                .histogramNativeMaxNumberOfBuckets(1)
                .build()
                .getHistogramNativeMaxNumberOfBuckets())
        .isOne();
    assertThat(
            MetricsProperties.builder()
                .histogramNativeResetDurationSeconds(1L)
                .build()
                .getHistogramNativeResetDurationSeconds())
        .isOne();

    MetricsProperties.Builder builder = MetricsProperties.builder();
    builder.histogramNativeInitialSchema(10);
    MetricsProperties.Builder builderWithInvalidValue = builder;
    assertThatExceptionOfType(PrometheusPropertiesException.class)
        .isThrownBy(builderWithInvalidValue::build)
        .withMessage(
            ".histogramNativeInitialSchema: Expecting number between -4 and +8. Found: 10");

    MetricsProperties.Builder builder1 = MetricsProperties.builder();
    builder1.histogramNativeMinZeroThreshold(-1.0);
    MetricsProperties.Builder builder1WithInvalidValue = builder1;
    assertThatExceptionOfType(PrometheusPropertiesException.class)
        .isThrownBy(builder1WithInvalidValue::build)
        .withMessage(".histogramNativeMinZeroThreshold: Expecting value >= 0. Found: -1.0");

    MetricsProperties.Builder builder2 = MetricsProperties.builder();
    builder2.histogramNativeMaxZeroThreshold(-1.0);
    MetricsProperties.Builder builder2WithInvalidValue = builder2;
    assertThatExceptionOfType(PrometheusPropertiesException.class)
        .isThrownBy(builder2WithInvalidValue::build)
        .withMessage(".histogramNativeMaxZeroThreshold: Expecting value >= 0. Found: -1.0");

    MetricsProperties.Builder builder3 = MetricsProperties.builder();
    builder3.histogramNativeMaxNumberOfBuckets(-1);
    MetricsProperties.Builder builder3WithInvalidValue = builder3;
    assertThatExceptionOfType(PrometheusPropertiesException.class)
        .isThrownBy(builder3WithInvalidValue::build)
        .withMessage(".histogramNativeMaxNumberOfBuckets: Expecting value >= 0. Found: -1");

    MetricsProperties.Builder builder4 = MetricsProperties.builder();
    builder4.histogramNativeResetDurationSeconds(-1L);
    MetricsProperties.Builder builder4WithInvalidValue = builder4;
    assertThatExceptionOfType(PrometheusPropertiesException.class)
        .isThrownBy(builder4WithInvalidValue::build)
        .withMessage(".histogramNativeResetDurationSeconds: Expecting value >= 0. Found: -1");

    MetricsProperties.Builder builder5 = MetricsProperties.builder();
    builder5.histogramNativeOnly(true);
    builder5.histogramClassicOnly(true);
    MetricsProperties.Builder builder5WithInvalidValue = builder5;
    assertThatExceptionOfType(PrometheusPropertiesException.class)
        .isThrownBy(builder5WithInvalidValue::build)
        .withMessage(".histogramNativeOnly and .histogramClassicOnly cannot both be true");

    MetricsProperties.Builder builder6 = MetricsProperties.builder();
    builder6.histogramNativeMinZeroThreshold(0.1);
    builder6.histogramNativeMaxZeroThreshold(0.01);
    MetricsProperties.Builder builder6WithInvalidValue = builder6;
    assertThatExceptionOfType(PrometheusPropertiesException.class)
        .isThrownBy(builder6WithInvalidValue::build)
        .withMessage(
            ".histogramNativeMinZeroThreshold cannot be greater than"
                + " .histogramNativeMaxZeroThreshold");
  }
}
