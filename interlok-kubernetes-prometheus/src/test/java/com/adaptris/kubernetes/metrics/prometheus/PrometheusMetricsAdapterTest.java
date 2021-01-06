package com.adaptris.kubernetes.metrics.prometheus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;

public class PrometheusMetricsAdapterTest {

  private PrometheusMetricsAdapter adapter;

  @Mock private PushGateway mockPushGateway;

  @Mock private MessageMetricsCollector mockMetricsCollctor;

  @Mock private MetricsCalculator mockCalculator;

  private AutoCloseable closeable;

  @BeforeEach
  public void setUp() throws Exception {
    closeable = MockitoAnnotations.openMocks(this);

    adapter = new PrometheusMetricsAdapter();
    adapter.setPushGateway(mockPushGateway);
  }

  @AfterEach
  public void tearDown() throws Exception {
    closeable.close();
  }

  @Test
  public void testInitLoadPropertiesDefauls() throws Exception {
    System.setProperty("K8S_NAMESPACE", "");
    System.setProperty("K8S_POD_NAME", "");
    adapter.init();

    assertEquals("default", adapter.getMetricLabels().get("k8s_namespace"));
    assertEquals("interlok", adapter.getMetricLabels().get("k8s_pod_name"));

    assertEquals(PrometheusMetricsAdapter.class.getSimpleName(), adapter.getImplementationName());
  }

  @Test
  public void testInitLoadPropertiesSystem() throws Exception {
    System.setProperty("K8S_NAMESPACE", "system_namespace");
    System.setProperty("K8S_POD_NAME", "system_pod_name");

    adapter.init();

    assertEquals("system_namespace", adapter.getMetricLabels().get("k8s_namespace"));
    assertEquals("system_pod_name", adapter.getMetricLabels().get("k8s_pod_name"));
  }

  @Test
  public void testInitLoadEndpointPropertiesSystem() throws Exception {
    System.setProperty("prometheusEndpointUrl", "localhost:9090");

    adapter.setPushGateway(null);
    adapter.init();

    Field field = adapter.getPushGateway().getClass().getDeclaredField("gatewayBaseURL");
    field.setAccessible(true);

    Object value = field.get(adapter.getPushGateway());

    assertEquals("http://localhost:9090/metrics/", value);
  }

  @Test
  public void testInitLoadEndpointBootstrap() throws Exception {
    System.setProperty("prometheusEndpointUrl", "");

    Properties bootstrapProperties = new Properties();
    bootstrapProperties.put("prometheusEndpointUrl", "localhost:9095");

    adapter.setBootstrapProperties(bootstrapProperties);
    adapter.setPushGateway(null);
    adapter.init();

    Field field = adapter.getPushGateway().getClass().getDeclaredField("gatewayBaseURL");
    field.setAccessible(true);

    Object value = field.get(adapter.getPushGateway());

    assertEquals("http://localhost:9095/metrics/", value);
  }

  @Test
  public void testInitNoLoadEndpoint() throws Exception {
    System.setProperty("prometheusEndpointUrl", "");

    adapter.setPushGateway(null);
    adapter.init();

    assertNull(adapter.getPushGateway());
  }

  @Test
  public void testStartPullsMetrics() throws Exception {
    adapter.setCollectorIntervalSeconds(1);
    adapter.setMessageMetricsCollector(mockMetricsCollctor);

    adapter.init();
    adapter.start();

    Thread.sleep(2000);

    adapter.stop();
    adapter.close();

    verify(mockMetricsCollctor, atLeast(1)).run();
  }

  @Test
  public void testSendToPrometheus() throws Exception {
    List<MessageStatisticExtended> statistics = new ArrayList<>();
    MessageStatisticExtended stat = new MessageStatisticExtended();
    stat.setStatisticId("MyStatId");
    statistics.add(stat);

    when(mockCalculator.calculateMessagesPerSecond(10l, stat))
    .thenReturn(10l);

    adapter.setCalculator(mockCalculator);
    adapter.init();

    adapter.notifyMessageMetrics(statistics);

    verify(mockPushGateway).pushAdd(any(CollectorRegistry.class), any(String.class), any(HashMap.class));
  }

  @Test
  public void testDoesNotSendToPrometheus() throws Exception {
    List<MessageStatisticExtended> statistics = new ArrayList<>();
    MessageStatisticExtended stat = new MessageStatisticExtended();
    stat.setStatisticId("MyStatId");
    statistics.add(stat);

    when(mockCalculator.calculateMessagesPerSecond(10l, stat))
    .thenReturn(-1l);

    adapter.setCalculator(mockCalculator);
    adapter.init();

    adapter.notifyMessageMetrics(statistics);

    verify(mockPushGateway, never()).pushAdd(any(CollectorRegistry.class), any(String.class), any(HashMap.class));
  }


}
