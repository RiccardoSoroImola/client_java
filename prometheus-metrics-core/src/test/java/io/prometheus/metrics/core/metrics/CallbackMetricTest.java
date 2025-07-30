package io.prometheus.metrics.core.metrics;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

class CallbackMetricTest {

  @Test
  void makeLabels() {
    CounterWithCallback obj =
        CounterWithCallback.builder()
            .name("c")
            .callback(callback -> {})
            .labelNames("label1", "label2")
            .build();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> obj.makeLabels("foo"))
        .withMessage(
            "CounterWithCallback was created with 2 label names, but the callback was called with 1"
                + " label values.");

    CounterWithCallback obj2 =
        CounterWithCallback.builder()
            .name("c")
            .callback(callback -> {})
            .labelNames("label1", "label2")
            .build();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> obj2.makeLabels((String[]) null))
        .withMessage(
            "CounterWithCallback was created with label names, but the callback was called without"
                + " label values.");

    CounterWithCallback obj3 =
        CounterWithCallback.builder().name("c").callback(callback -> {}).build();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> obj3.makeLabels("foo"))
        .withMessage(
            "Cannot pass label values to a CounterWithCallback that was created without label"
                + " names.");
  }
}
