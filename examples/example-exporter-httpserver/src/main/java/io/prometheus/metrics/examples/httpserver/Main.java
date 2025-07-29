package io.prometheus.metrics.examples.httpserver;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.snapshots.Unit;
import java.io.IOException;
import java.util.logging.Logger;

/** Simple example of an application exposing metrics via Prometheus' built-in HTTPServer. */
public class Main {

  public static void main(String[] args) throws IOException, InterruptedException {

    JvmMetrics.builder().register();

    // Note: uptime_seconds_total is not a great example:
    // The built-in JvmMetrics have an out-of-the-box metric named process_start_time_seconds
    // with the start timestamp in seconds, so if you want to know the uptime you can simply
    // run the Prometheus query
    //     time() - process_start_time_seconds
    // rather than creating a custom uptime metric.
    Counter counter =
        Counter.builder()
            .name("uptime_seconds_total")
            .help("total number of seconds since this application was started")
            .unit(Unit.SECONDS)
            .register();

    HTTPServer server = HTTPServer.builder().port(9400).buildAndStart();

    Logger logger = Logger.getLogger(Main.class.getName());
    logger.info("HTTPServer listening on port http://localhost:" + server.getPort() + "/metrics");

    while (!Thread.currentThread().isInterrupted()) {
      Thread.sleep(1000);
      counter.inc();
    }
  }
}
