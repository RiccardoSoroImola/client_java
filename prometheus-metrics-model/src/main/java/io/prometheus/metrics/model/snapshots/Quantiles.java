package io.prometheus.metrics.model.snapshots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** Immutable list of quantiles. */
public class Quantiles implements Iterable<Quantile> {

  private final List<Quantile> quantileList;
  public static final Quantiles EMPTY = new Quantiles(Collections.emptyList());

  private Quantiles(List<Quantile> quantiles) {
    quantiles = new ArrayList<>(quantiles);
    quantiles.sort(Comparator.comparing(Quantile::getQuantile));
    this.quantileList = Collections.unmodifiableList(quantiles);
    validate();
  }

  private void validate() {
    for (int i = 0; i < quantileList.size() - 1; i++) {
      if (quantileList.get(i).getQuantile() == quantileList.get(i + 1).getQuantile()) {
        throw new IllegalArgumentException(
            "Duplicate " + quantileList.get(i).getQuantile() + " quantile.");
      }
    }
  }

  /**
   * Create a new Quantiles instance. You can either create Quantiles with one of the static {@code
   * Quantiles.of(...)} methods, or you can use the {@link Quantiles#builder()}.
   */
  public static Quantiles of(List<Quantile> quantiles) {
    return new Quantiles(quantiles);
  }

  /**
   * Create a new Quantiles instance. You can either create Quantiles with one of the static {@code
   * Quantiles.of(...)} methods, or you can use the {@link Quantiles#builder()}.
   */
  public static Quantiles of(Quantile... quantiles) {
    return of(Arrays.asList(quantiles));
  }

  public int size() {
    return quantileList.size();
  }

  public Quantile get(int i) {
    return quantileList.get(i);
  }

  @Override
  public Iterator<Quantile> iterator() {
    return quantileList.iterator();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private final List<Quantile> quantileEntries = new ArrayList<>();

    private Builder() {}

    /** Add a quantile. Call multiple times to add multiple quantiles. */
    public Builder quantile(Quantile quantile) {
      quantileEntries.add(quantile);
      return this;
    }

    /**
     * Add a quantile. Call multiple times to add multiple quantiles.
     *
     * @param quantile 0.0 &lt;= quantile &lt;= 1.0
     * @param value the quantile value
     */
    public Builder quantile(double quantile, double value) {
      quantileEntries.add(new Quantile(quantile, value));
      return this;
    }

    public Quantiles build() {
      return new Quantiles(quantileEntries);
    }
  }
}
