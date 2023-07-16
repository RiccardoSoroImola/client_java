package io.prometheus.metrics.core.metrics;

import io.prometheus.metrics.config.MetricsProperties;
import io.prometheus.metrics.config.PrometheusProperties;
import io.prometheus.metrics.core.exemplars.ExemplarSampler;
import io.prometheus.metrics.core.exemplars.ExemplarSamplerConfig;
import io.prometheus.metrics.model.snapshots.Exemplars;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.Quantile;
import io.prometheus.metrics.model.snapshots.Quantiles;
import io.prometheus.metrics.model.snapshots.SummarySnapshot;
import io.prometheus.metrics.core.datapoints.DistributionDataPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * Summary metric. Example:
 * <pre>{@code
 * Summary summary = Summary.newBuilder()
 *         .withName("http_request_duration_seconds_hi")
 *         .withHelp("HTTP request service time in seconds")
 *         .withUnit(SECONDS)
 *         .withLabelNames("method", "path", "status_code")
 *         .withQuantile(0.5, 0.01)
 *         .withQuantile(0.95, 0.001)
 *         .withQuantile(0.99, 0.001)
 *         .register();
 *
 * long start = System.nanoTime();
 * // process a request, duration will be observed
 * summary.withLabelValues("GET", "/", "200").observe(Unit.nanosToSeconds(System.nanoTime() - start));
 * }</pre>
 * See {@link Summary.Builder} for configuration options.
 */
public class Summary extends StatefulMetric<DistributionDataPoint, Summary.DataPoint> implements DistributionDataPoint {

    private final List<CKMSQuantiles.Quantile> quantiles; // May be empty, but cannot be null.
    private final long maxAgeSeconds;
    private final int ageBuckets;
    private final boolean exemplarsEnabled;
    private final ExemplarSamplerConfig exemplarSamplerConfig;

    private Summary(Builder builder, PrometheusProperties prometheusProperties) {
        super(builder);
        MetricsProperties[] properties = getMetricProperties(builder, prometheusProperties);
        this.exemplarsEnabled = getConfigProperty(properties, MetricsProperties::getExemplarsEnabled);
        this.quantiles = Collections.unmodifiableList(makeQuantiles(properties));
        this.maxAgeSeconds = getConfigProperty(properties, MetricsProperties::getSummaryMaxAgeSeconds);
        this.ageBuckets = getConfigProperty(properties, MetricsProperties::getSummaryNumberOfAgeBuckets);
        this.exemplarSamplerConfig = new ExemplarSamplerConfig(prometheusProperties.getExemplarProperties(), 4);
    }

    private List<CKMSQuantiles.Quantile> makeQuantiles(MetricsProperties[] properties) {
        List<CKMSQuantiles.Quantile> result = new ArrayList<>();
        List<Double> quantiles = getConfigProperty(properties, MetricsProperties::getSummaryQuantiles);
        List<Double> quantileErrors = getConfigProperty(properties, MetricsProperties::getSummaryQuantileErrors);
        if (quantiles != null) {
            for (int i = 0; i < quantiles.size(); i++) {
                if (quantileErrors.size() > 0) {
                    result.add(new CKMSQuantiles.Quantile(quantiles.get(i), quantileErrors.get(i)));
                } else {
                    result.add(new CKMSQuantiles.Quantile(quantiles.get(i), Builder.defaultError(quantiles.get(i))));
                }
            }
        }
        return result;
    }

    @Override
    protected boolean isExemplarsEnabled() {
        return exemplarsEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void observe(double amount) {
        getNoLabels().observe(amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void observeWithExemplar(double amount, Labels labels) {
        getNoLabels().observeWithExemplar(amount, labels);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SummarySnapshot collect() {
        return (SummarySnapshot) super.collect();
    }

    @Override
    protected SummarySnapshot collect(List<Labels> labels, List<DataPoint> metricData) {
        List<SummarySnapshot.SummaryDataPointSnapshot> data = new ArrayList<>(labels.size());
        for (int i = 0; i < labels.size(); i++) {
            data.add(metricData.get(i).collect(labels.get(i)));
        }
        return new SummarySnapshot(getMetadata(), data);
    }

    @Override
    protected DataPoint newDataPoint() {
        return new DataPoint();
    }


    public class DataPoint implements DistributionDataPoint {

        private final LongAdder count = new LongAdder();
        private final DoubleAdder sum = new DoubleAdder();
        private final SlidingWindow<CKMSQuantiles> quantileValues;
        private final Buffer buffer = new Buffer();
        private final ExemplarSampler exemplarSampler;

        private final long createdTimeMillis = System.currentTimeMillis();

        private DataPoint() {
            if (quantiles.size() > 0) {
                CKMSQuantiles.Quantile[] quantilesArray = quantiles.toArray(new CKMSQuantiles.Quantile[0]);
                quantileValues = new SlidingWindow<>(CKMSQuantiles.class, () -> new CKMSQuantiles(quantilesArray), CKMSQuantiles::insert, maxAgeSeconds, ageBuckets);
            } else {
                quantileValues = null;
            }
            if (exemplarsEnabled) {
                exemplarSampler = new ExemplarSampler(exemplarSamplerConfig);
            } else {
                exemplarSampler = null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void observe(double value) {
            if (Double.isNaN(value)) {
                return;
            }
            if (!buffer.append(value)) {
                doObserve(value);
            }
            if (isExemplarsEnabled()) {
                exemplarSampler.observe(value);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void observeWithExemplar(double value, Labels labels) {
            if (Double.isNaN(value)) {
                return;
            }
            if (!buffer.append(value)) {
                doObserve(value);
            }
            if (isExemplarsEnabled()) {
                exemplarSampler.observeWithExemplar(value, labels);
            }
        }

        private void doObserve(double amount) {
            sum.add(amount);
            if (quantileValues != null) {
                quantileValues.observe(amount);
            }
            // count must be incremented last, because in collect() the count
            // indicates the number of completed observations.
            count.increment();
        }

        private SummarySnapshot.SummaryDataPointSnapshot collect(Labels labels) {
            return buffer.run(
                    expectedCount -> count.sum() == expectedCount,
                    // TODO Exemplars (are hard-coded as empty in the line below)
                    () -> new SummarySnapshot.SummaryDataPointSnapshot(count.sum(), sum.sum(), makeQuantiles(), labels, Exemplars.EMPTY, createdTimeMillis),
                    this::doObserve
            );
        }

        private List<CKMSQuantiles.Quantile> getQuantiles() {
            return quantiles;
        }

        private Quantiles makeQuantiles() {
            Quantile[] quantiles = new Quantile[getQuantiles().size()];
            for (int i = 0; i < getQuantiles().size(); i++) {
                CKMSQuantiles.Quantile quantile = getQuantiles().get(i);
                quantiles[i] = new Quantile(quantile.quantile, quantileValues.current().get(quantile.quantile));
            }
            return Quantiles.of(quantiles);
        }
    }

    public static Summary.Builder newBuilder() {
        return new Builder(PrometheusProperties.get());
    }

    public static Summary.Builder newBuilder(PrometheusProperties config) {
        return new Builder(config);
    }

    public static class Builder extends StatefulMetric.Builder<Summary.Builder, Summary> {

        /**
         * 5 minutes. See {@link #withMaxAgeSeconds(long)}.
         */
        public static final long DEFAULT_MAX_AGE_SECONDS = TimeUnit.MINUTES.toSeconds(5);

        /**
         * 5. See {@link #withNumberOfAgeBuckets(int)}
         */
        public static final int DEFAULT_NUMBER_OF_AGE_BUCKETS = 5;
        private final List<CKMSQuantiles.Quantile> quantiles = new ArrayList<>();
        private Long maxAgeSeconds;
        private Integer ageBuckets;

        private Builder(PrometheusProperties properties) {
            super(Collections.singletonList("quantile"), properties);
        }

        private static double defaultError(double quantile) {
            if (quantile <= 0.01 || quantile >= 0.99) {
                return 0.001;
            } else if (quantile <= 0.02 || quantile >= 0.98) {
                return 0.005;
            } else {
                return 0.01;
            }
        }

        /**
         * See {@link #withQuantile(double, double)}.
         * <p>
         * Default errors are:
         * <ul>
         *     <li>error = 0.001 if quantile &lt;= 0.01 or quantile &gt;= 0.99</li>
         *     <li>error = 0.005 if quantile &lt;= 0.02 or quantile &gt;= 0.98</li>
         *     <li>error = 0.01 else.
         * </ul>
         */
        public Builder withQuantile(double quantile) {
            return withQuantile(quantile, defaultError(quantile));
        }

        /**
         * Example: The following will track the 0.95 quantile:
         * <pre>{@code
         * .withQuantile(0.95, 0.001)
         * }</pre>
         * The second argument is the acceptable error margin, i.e. with the code above the quantile
         * will not be exactly the 0.95 quantile but something between 0.949 and 0.951.
         * <p>
         * There are two special cases:
         * <ul>
         *     <li>{@code .withQuantile(0.0, 0.0)} gives you the minimum observed value</li>
         *     <li>{@code .withQuantile(1.0, 0.0)} gives you the maximum observed value</li>
         * </ul>
         */
        public Builder withQuantile(double quantile, double error) {
            if (quantile < 0.0 || quantile > 1.0) {
                throw new IllegalArgumentException("Quantile " + quantile + " invalid: Expected number between 0.0 and 1.0.");
            }
            if (error < 0.0 || error > 1.0) {
                throw new IllegalArgumentException("Error " + error + " invalid: Expected number between 0.0 and 1.0.");
            }
            quantiles.add(new CKMSQuantiles.Quantile(quantile, error));
            return this;
        }

        /**
         * The quantiles are relative to a moving time window.
         * {@code maxAgeSeconds} is the size of that time window.
         * Default is {@link #DEFAULT_MAX_AGE_SECONDS}.
         */
        public Builder withMaxAgeSeconds(long maxAgeSeconds) {
            if (maxAgeSeconds <= 0) {
                throw new IllegalArgumentException("maxAgeSeconds cannot be " + maxAgeSeconds);
            }
            this.maxAgeSeconds = maxAgeSeconds;
            return this;
        }

        /**
         * The quantiles are relative to a moving time window.
         * The {@code numberOfAgeBuckets} defines how smoothly the time window moves forward.
         * For example, if the time window is 5 minutes and has 5 age buckets,
         * then it is moving forward every minute by one minute.
         * Default is {@link #DEFAULT_NUMBER_OF_AGE_BUCKETS}.
         */
        public Builder withNumberOfAgeBuckets(int ageBuckets) {
            if (ageBuckets <= 0) {
                throw new IllegalArgumentException("ageBuckets cannot be " + ageBuckets);
            }
            this.ageBuckets = ageBuckets;
            return this;
        }

        @Override
        protected MetricsProperties toProperties() {
            double[] quantiles = null;
            double[] quantileErrors = null;
            if (!this.quantiles.isEmpty()) {
                quantiles = new double[this.quantiles.size()];
                quantileErrors = new double[this.quantiles.size()];
                for (int i = 0; i < this.quantiles.size(); i++) {
                    quantiles[i] = this.quantiles.get(i).quantile;
                    quantileErrors[i] = this.quantiles.get(i).epsilon;
                }
            }
            return MetricsProperties.newBuilder()
                    .withExemplarsEnabled(exemplarsEnabled)
                    .withSummaryQuantiles(quantiles)
                    .withSummaryQuantileErrors(quantileErrors)
                    .withSummaryNumberOfAgeBuckets(ageBuckets)
                    .withSummaryMaxAgeSeconds(maxAgeSeconds)
                    .build();
        }

        /**
         * Default properties for summary metrics.
         */
        @Override
        public MetricsProperties getDefaultProperties() {
            return MetricsProperties.newBuilder()
                    .withExemplarsEnabled(true)
                    .withSummaryQuantiles()
                    .withSummaryNumberOfAgeBuckets(DEFAULT_NUMBER_OF_AGE_BUCKETS)
                    .withSummaryMaxAgeSeconds(DEFAULT_MAX_AGE_SECONDS)
                    .build();
        }

        @Override
        public Summary build() {
            return new Summary(this, properties);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
