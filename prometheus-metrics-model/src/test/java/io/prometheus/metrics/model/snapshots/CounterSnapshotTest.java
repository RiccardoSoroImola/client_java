package io.prometheus.metrics.model.snapshots;

import io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class CounterSnapshotTest {

    @Test
    public void testCompleteGoodCase() {
        long createdTimestamp1 = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        long createdTimestamp2 = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(2);
        long exemplarTimestamp = System.currentTimeMillis();
        CounterSnapshot snapshot = CounterSnapshot.newBuilder()
                .withName("http_server_requests_seconds")
                .withHelp("total time spent serving requests")
                .withUnit(Unit.SECONDS)
                .addDataPoint(CounterDataPointSnapshot.newBuilder()
                        .withValue(1.0)
                        .withExemplar(Exemplar.newBuilder()
                                .withValue(3.0)
                                .withTraceId("abc123")
                                .withSpanId("123457")
                                .withTimestampMillis(exemplarTimestamp)
                                .build())
                        .withLabels(Labels.newBuilder()
                                .addLabel("path", "/world")
                                .build())
                        .withCreatedTimestampMillis(createdTimestamp1)
                        .build()
                ).addDataPoint(CounterDataPointSnapshot.newBuilder()
                        .withValue(2.0)
                        .withExemplar(Exemplar.newBuilder()
                                .withValue(4.0)
                                .withTraceId("def456")
                                .withSpanId("234567")
                                .withTimestampMillis(exemplarTimestamp)
                                .build())
                        .withLabels(Labels.newBuilder()
                                .addLabel("path", "/hello")
                                .build())
                        .withCreatedTimestampMillis(createdTimestamp2)
                        .build()
                )
                .build();
        SnapshotTestUtil.assertMetadata(snapshot, "http_server_requests_seconds", "total time spent serving requests", "seconds");
        Assert.assertEquals(2, snapshot.getData().size());
        CounterDataPointSnapshot data = snapshot.getData().get(0); // data is sorted by labels, so the first one should be path="/hello"
        Assert.assertEquals(Labels.of("path", "/hello"), data.getLabels());
        Assert.assertEquals(2.0, data.getValue(), 0.0);
        Assert.assertEquals(4.0, data.getExemplar().getValue(), 0.0);
        Assert.assertEquals(createdTimestamp2, data.getCreatedTimestampMillis());
        Assert.assertFalse(data.hasScrapeTimestamp());
        data = snapshot.getData().get(1);
        Assert.assertEquals(Labels.of("path", "/world"), data.getLabels());
        Assert.assertEquals(1.0, data.getValue(), 0.0);
        Assert.assertEquals(3.0, data.getExemplar().getValue(), 0.0);
        Assert.assertEquals(createdTimestamp1, data.getCreatedTimestampMillis());
        Assert.assertFalse(data.hasScrapeTimestamp());
    }

    @Test
    public void testMinimalGoodCase() {
        CounterSnapshot snapshot = CounterSnapshot.newBuilder()
                .withName("events")
                .addDataPoint(CounterDataPointSnapshot.newBuilder().withValue(1.0).build())
                .build();
        SnapshotTestUtil.assertMetadata(snapshot, "events", null, null);
        Assert.assertEquals(1, snapshot.getData().size());
        CounterDataPointSnapshot data = snapshot.getData().get(0);
        Assert.assertEquals(Labels.EMPTY, data.getLabels());
        Assert.assertEquals(1.0, data.getValue(), 0.0);
        Assert.assertNull(data.getExemplar());
        Assert.assertFalse(data.hasCreatedTimestamp());
        Assert.assertFalse(data.hasScrapeTimestamp());
    }

    @Test
    public void testEmptyCounter() {
        CounterSnapshot snapshot = CounterSnapshot.newBuilder().withName("events").build();
        Assert.assertEquals(0, snapshot.getData().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTotalSuffixPresent() {
        CounterSnapshot.newBuilder().withName("test_total").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueMissing() {
        CounterDataPointSnapshot.newBuilder().build();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDataImmutable() {
        CounterSnapshot snapshot = CounterSnapshot.newBuilder()
                .withName("events")
                .addDataPoint(CounterDataPointSnapshot.newBuilder().withLabels(Labels.of("a", "a")).withValue(1.0).build())
                .addDataPoint(CounterDataPointSnapshot.newBuilder().withLabels(Labels.of("a", "b")).withValue(2.0).build())
                .build();
        Iterator<CounterDataPointSnapshot> iterator = snapshot.getData().iterator();
        iterator.next();
        iterator.remove();
    }
}
