package io.prometheus.metrics.exporter.opentelemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.prometheus.metrics.exporter.opentelemetry.otelmodel.MetricDataFactory;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.HistogramSnapshot;
import io.prometheus.metrics.model.snapshots.InfoSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.prometheus.metrics.model.snapshots.StateSetSnapshot;
import io.prometheus.metrics.model.snapshots.SummarySnapshot;
import io.prometheus.metrics.model.snapshots.UnknownSnapshot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class PrometheusMetricProducer implements CollectionRegistration {

  private final PrometheusRegistry registry;
  private final Resource resource;
  private final InstrumentationScopeInfo instrumentationScopeInfo;

  public PrometheusMetricProducer(
      PrometheusRegistry registry,
      InstrumentationScopeInfo instrumentationScopeInfo,
      Resource resource) {
    this.registry = registry;
    this.instrumentationScopeInfo = instrumentationScopeInfo;
    this.resource = resource;
  }

  @Override
  public Collection<MetricData> collectAllMetrics() {
    MetricSnapshots snapshots = registry.scrape();
    Resource resourceWithTargetInfo = resource.merge(resourceFromTargetInfo(snapshots));
    InstrumentationScopeInfo scopeFromInfo = instrumentationScopeFromOtelScopeInfo(snapshots);
    List<MetricData> result = new ArrayList<>(snapshots.size());
    MetricDataFactory factory =
        new MetricDataFactory(
            resourceWithTargetInfo,
            scopeFromInfo != null ? scopeFromInfo : instrumentationScopeInfo,
            System.currentTimeMillis());
    for (MetricSnapshot snapshot : snapshots) {
      MetricData data = null;
      if (snapshot instanceof CounterSnapshot) {
        data = handleCounter((CounterSnapshot) snapshot, factory);
      } else if (snapshot instanceof GaugeSnapshot) {
        data = handleGauge((GaugeSnapshot) snapshot, factory);
      } else if (snapshot instanceof HistogramSnapshot) {
        data = handleHistogram((HistogramSnapshot) snapshot, factory);
      } else if (snapshot instanceof SummarySnapshot) {
        data = handleSummary((SummarySnapshot) snapshot, factory);
      } else if (snapshot instanceof InfoSnapshot) {
        data = handleInfo((InfoSnapshot) snapshot, factory);
      } else if (snapshot instanceof StateSetSnapshot) {
        data = handleStateSet((StateSetSnapshot) snapshot, factory);
      } else if (snapshot instanceof UnknownSnapshot) {
        data = handleUnknown((UnknownSnapshot) snapshot, factory);
      }
      addUnlessNull(result, data);
    }
    return result;
  }

  private MetricData handleCounter(CounterSnapshot snapshot, MetricDataFactory factory) {
    return factory.create(snapshot);
  }

  private MetricData handleGauge(GaugeSnapshot snapshot, MetricDataFactory factory) {
    return factory.create(snapshot);
  }

  private MetricData handleHistogram(HistogramSnapshot snapshot, MetricDataFactory factory) {
    if (!snapshot.isGaugeHistogram()) {
      return factory.create(snapshot);
    }
    return null;
  }

  private MetricData handleSummary(SummarySnapshot snapshot, MetricDataFactory factory) {
    return factory.create(snapshot);
  }

  private MetricData handleInfo(InfoSnapshot snapshot, MetricDataFactory factory) {
    String name = snapshot.getMetadata().getPrometheusName();
    if (!name.equals("target") && !name.equals("otel_scope")) {
      return factory.create(snapshot);
    }
    return null;
  }

  private MetricData handleStateSet(StateSetSnapshot snapshot, MetricDataFactory factory) {
    return factory.create(snapshot);
  }

  private MetricData handleUnknown(UnknownSnapshot snapshot, MetricDataFactory factory) {
    return factory.create(snapshot);
  }

  private Resource resourceFromTargetInfo(MetricSnapshots snapshots) {
    ResourceBuilder result = Resource.builder();
    InfoSnapshot targetInfo = findTargetInfoSnapshot(snapshots);
    if (targetInfo != null) {
      InfoSnapshot.InfoDataPointSnapshot data = targetInfo.getDataPoints().get(0);
      Labels labels = data.getLabels();
      for (int i = 0; i < labels.size(); i++) {
        result.put(labels.getName(i), labels.getValue(i));
      }
    }
    return result.build();
  }

  private InfoSnapshot findTargetInfoSnapshot(MetricSnapshots snapshots) {
    for (MetricSnapshot snapshot : snapshots) {
      if (snapshot.getMetadata().getName().equals("target") && snapshot instanceof InfoSnapshot) {
        InfoSnapshot targetInfo = (InfoSnapshot) snapshot;
        if (!targetInfo.getDataPoints().isEmpty()) {
          return targetInfo;
        }
      }
    }
    return null;
  }

  private InstrumentationScopeInfo instrumentationScopeFromOtelScopeInfo(
      MetricSnapshots snapshots) {
    InfoSnapshot scopeInfo = findOtelScopeInfoSnapshot(snapshots);
    if (scopeInfo == null) {
      return null;
    }
    Labels labels = scopeInfo.getDataPoints().get(0).getLabels();
    String name = null;
    String version = null;
    AttributesBuilder attributesBuilder = Attributes.builder();
    for (int i = 0; i < labels.size(); i++) {
      if (labels.getPrometheusName(i).equals("otel_scope_name")) {
        name = labels.getValue(i);
      } else if (labels.getPrometheusName(i).equals("otel_scope_version")) {
        version = labels.getValue(i);
      } else {
        attributesBuilder.put(labels.getName(i), labels.getValue(i));
      }
    }
    if (name != null) {
      return InstrumentationScopeInfo.builder(name)
          .setVersion(version)
          .setAttributes(attributesBuilder.build())
          .build();
    }
    return null;
  }

  private InfoSnapshot findOtelScopeInfoSnapshot(MetricSnapshots snapshots) {
    for (MetricSnapshot snapshot : snapshots) {
      if (snapshot.getMetadata().getPrometheusName().equals("otel_scope")
          && snapshot instanceof InfoSnapshot) {
        InfoSnapshot scopeInfo = (InfoSnapshot) snapshot;
        if (!scopeInfo.getDataPoints().isEmpty()) {
          return scopeInfo;
        }
      }
    }
    return null;
  }

  private void addUnlessNull(List<MetricData> result, MetricData data) {
    if (data != null) {
      result.add(data);
    }
  }
}
