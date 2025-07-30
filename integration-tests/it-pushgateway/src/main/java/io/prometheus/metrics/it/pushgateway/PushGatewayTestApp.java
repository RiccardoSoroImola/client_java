package io.prometheus.metrics.it.pushgateway;

import static io.prometheus.metrics.exporter.pushgateway.Scheme.HTTPS;

import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.exporter.pushgateway.Format;
import io.prometheus.metrics.exporter.pushgateway.HttpConnectionFactory;
import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import io.prometheus.metrics.model.snapshots.Unit;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/** Example application using the {@link PushGateway}. */
class PushGatewayTestApp {

  private static final Logger logger = Logger.getLogger(PushGatewayTestApp.class.getName());
  private static final String PUSHING_METRICS = "Pushing metrics...";
  private static final String PUSH_SUCCESSFUL = "Push successful.";

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      logger.severe("Usage: java -jar pushgateway-test-app.jar <test>");
      System.exit(-1);
    }
    switch (args[0]) {
      case "simple":
        runSimpleTest();
        break;
      case "textFormat":
        runTextFormatTest();
        break;
      case "basicauth":
        runBasicAuthTest();
        break;
      case "ssl":
        runSslTest();
        break;
      default:
        if (logger.isLoggable(Level.SEVERE)) {
          logger.severe(args[0] + ": Not implemented.");
        }
        System.exit(-1);
    }
  }

  private static void runSimpleTest() throws IOException {
    makeMetrics();
    PushGateway pg = PushGateway.builder().build();
    logger.info(PUSHING_METRICS);
    pg.push();
    logger.info(PUSH_SUCCESSFUL);
  }

  private static void runTextFormatTest() throws IOException {
    makeMetrics();
    PushGateway pg = PushGateway.builder().format(Format.PROMETHEUS_TEXT).build();
    logger.info(PUSHING_METRICS);
    pg.push();
    logger.info(PUSH_SUCCESSFUL);
  }

  private static void runBasicAuthTest() throws IOException {
    makeMetrics();
    PushGateway pg = PushGateway.builder().basicAuth("my_user", "secret_password").build();
    logger.info(PUSHING_METRICS);
    pg.push();
    logger.info(PUSH_SUCCESSFUL);
  }

  private static void runSslTest() throws IOException {
    makeMetrics();
    PushGateway pg =
        PushGateway.builder().scheme(HTTPS).connectionFactory(insecureConnectionFactory).build();
    logger.info(PUSHING_METRICS);
    pg.push();
    logger.info(PUSH_SUCCESSFUL);
  }

  static HttpConnectionFactory insecureConnectionFactory =
      url -> {
        try {
          SSLContext sslContext = SSLContext.getInstance("TLS");
          sslContext.init(null, null, null);
          HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
          connection.setSSLSocketFactory(sslContext.getSocketFactory());
          connection.setHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
          return connection;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
          throw new RuntimeException(e);
        }
      };

  private static void makeMetrics() {
    Histogram sizes =
        Histogram.builder()
            .name("file_sizes_bytes")
            .classicUpperBounds(256, 512, 1024, 2048)
            .unit(Unit.BYTES)
            .register();
    sizes.observe(513);
    sizes.observe(814);
    sizes.observe(1553);
    Gauge duration =
        Gauge.builder()
            .name("my_batch_job_duration_seconds")
            .help("Duration of my batch job in seconds.")
            .unit(Unit.SECONDS)
            .register();
    duration.set(0.5);
  }
}
