package io.prometheus.metrics.model.snapshots;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UnitTest {

  @Test
  void testEmpty() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new Unit(" "));
  }

  @ParameterizedTest
  @CsvSource({"bytes, bytes", "bytes , bytes", " bytes, bytes", " bytes , bytes"})
  void testEquals(String input1, String input2) {
    Unit unit1 = new Unit(input1);
    Unit unit2 = new Unit(input2);

    assertThat(unit1).isEqualTo(unit2);
  }
}
