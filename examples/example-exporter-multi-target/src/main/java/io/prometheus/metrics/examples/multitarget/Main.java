package io.prometheus.metrics.examples.multitarget;

import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.IOException;
import java.util.logging.Logger;

/** Simple example of an application exposing metrics via Prometheus' built-in HTTPServer. */
public class Main {

  public static void main(String[] args) throws IOException {

    SampleMultiCollector xmc = new SampleMultiCollector();
    PrometheusRegistry.defaultRegistry.register(xmc);
    HTTPServer server = HTTPServer.builder().port(9401).buildAndStart();

    Logger logger = Logger.getLogger(Main.class.getName());
    logger.info("HTTPServer listening on port http://localhost:" + server.getPort() + "/metrics");
  }
}
