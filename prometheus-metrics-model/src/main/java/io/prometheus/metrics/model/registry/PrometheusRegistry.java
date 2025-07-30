package io.prometheus.metrics.model.registry;

import static io.prometheus.metrics.model.snapshots.PrometheusNaming.prometheusName;

import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public class PrometheusRegistry {

  public static final PrometheusRegistry defaultRegistry = new PrometheusRegistry();

  private final Set<String> prometheusNames = ConcurrentHashMap.newKeySet();
  private final List<Collector> collectors = new CopyOnWriteArrayList<>();
  private final List<MultiCollector> multiCollectors = new CopyOnWriteArrayList<>();

  public void register(Collector collector) {
    String prometheusName = collector.getPrometheusName();
    if (prometheusName != null && !prometheusNames.add(prometheusName)) {
      throw new IllegalStateException(
          "Can't register "
              + prometheusName
              + " because a metric with that name is already registered.");
    }
    collectors.add(collector);
  }

  public void register(MultiCollector collector) {
    for (String prometheusName : collector.getPrometheusNames()) {
      if (!prometheusNames.add(prometheusName)) {
        throw new IllegalStateException(
            "Can't register " + prometheusName + " because that name is already registered.");
      }
    }
    multiCollectors.add(collector);
  }

  public void unregister(Collector collector) {
    collectors.remove(collector);
    String prometheusName = collector.getPrometheusName();
    if (prometheusName != null) {
      prometheusNames.remove(collector.getPrometheusName());
    }
  }

  public void unregister(MultiCollector collector) {
    multiCollectors.remove(collector);
    for (String prometheusName : collector.getPrometheusNames()) {
      prometheusNames.remove(prometheusName(prometheusName));
    }
  }

  public void clear() {
    collectors.clear();
    multiCollectors.clear();
    prometheusNames.clear();
  }

  public MetricSnapshots scrape() {
    return scrape((PrometheusScrapeRequest) null);
  }

  public MetricSnapshots scrape(PrometheusScrapeRequest scrapeRequest) {
    MetricSnapshots.Builder result = MetricSnapshots.builder();
    for (Collector collector : collectors) {
      MetricSnapshot snapshot =
          scrapeRequest == null ? collector.collect() : collector.collect(scrapeRequest);
      if (snapshot != null) {
        if (result.containsMetricName(snapshot.getMetadata().getName())) {
          throw new IllegalStateException(
              snapshot.getMetadata().getPrometheusName() + ": duplicate metric name.");
        }
        result.metricSnapshot(snapshot);
      }
    }
    for (MultiCollector collector : multiCollectors) {
      MetricSnapshots snapshots =
          scrapeRequest == null ? collector.collect() : collector.collect(scrapeRequest);
      for (MetricSnapshot snapshot : snapshots) {
        if (snapshot != null) {
          if (result.containsMetricName(snapshot.getMetadata().getName())) {
            throw new IllegalStateException(
                snapshot.getMetadata().getPrometheusName() + ": duplicate metric name.");
          }
          result.metricSnapshot(snapshot);
        }
      }
    }
    return result.build();
  }

  public MetricSnapshots scrape(Predicate<String> includedNames) {
    if (includedNames == null) {
      return scrape();
    }
    return scrape(includedNames, null);
  }

  public MetricSnapshots scrape(
      Predicate<String> includedNames, PrometheusScrapeRequest scrapeRequest) {
    if (includedNames == null) {
      return scrape(scrapeRequest);
    }
    MetricSnapshots.Builder result = MetricSnapshots.builder();
    processCollectors(collectors, includedNames, scrapeRequest, result);
    processMultiCollectors(multiCollectors, includedNames, scrapeRequest, result);
    return result.build();
  }

  private void processCollectors(
      List<Collector> collectorList,
      Predicate<String> includedNames,
      PrometheusScrapeRequest scrapeRequest,
      MetricSnapshots.Builder result) {
    for (Collector collector : collectorList) {
      String prometheusName = collector.getPrometheusName();
      if (prometheusName == null || includedNames.test(prometheusName)) {
        MetricSnapshot snapshot =
            scrapeRequest == null
                ? collector.collect(includedNames)
                : collector.collect(includedNames, scrapeRequest);
        if (snapshot != null) {
          result.metricSnapshot(snapshot);
        }
      }
    }
  }

  private void processMultiCollectors(
      List<MultiCollector> multiCollectorList,
      Predicate<String> includedNames,
      PrometheusScrapeRequest scrapeRequest,
      MetricSnapshots.Builder result) {
    for (MultiCollector collector : multiCollectorList) {
      List<String> prometheusNames = collector.getPrometheusNames();
      if (isAnyNameIncluded(prometheusNames, includedNames)) {
        MetricSnapshots snapshots =
            scrapeRequest == null
                ? collector.collect(includedNames)
                : collector.collect(includedNames, scrapeRequest);
        for (MetricSnapshot snapshot : snapshots) {
          if (snapshot != null) {
            result.metricSnapshot(snapshot);
          }
        }
      }
    }
  }

  private boolean isAnyNameIncluded(List<String> names, Predicate<String> includedNames) {
    if (names.isEmpty()) {
      return true;
    }
    for (String name : names) {
      if (includedNames.test(name)) {
        return true;
      }
    }
    return false;
  }
}
