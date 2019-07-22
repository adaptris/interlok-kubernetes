package com.adaptris.kubernetes.metrics.prometheus;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.adaptris.core.CoreException;
import com.adaptris.mgmt.kubernetes.metrics.KubernetesMetricsAdapter;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.PushGateway;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrometheusMetricsAdapter implements KubernetesMetricsAdapter, MessageMetricsListener {

  private static final String PROMETHEUS_JOB_NAME = "Interlok";

  private static final String IMPLEMENTATION_NAME = PrometheusMetricsAdapter.class.getSimpleName(); 
  
  private static final String PROMETHEUS_ENDPOINT_KEY = "prometheusEndpointUrl";
  
  private static final Integer METRICS_COLLECTOR_INTERVAL_SECONDS_DEFAULT = 10;
  
  @Getter
  @Setter
  private Properties bootstrapProperties;
  
  @Getter
  @Setter
  private PushGateway pushGateway;
  
  @Getter
  @Setter
  private MessageMetricsCollector messageMetricsCollector;
  
  @Getter
  @Setter
  private Integer collectorIntervalSeconds;
  
  private ScheduledExecutorService scheduler;
  private ScheduledFuture<?> schedulerHandle;
  
  public PrometheusMetricsAdapter() {
    MessageMetricsCollector metricsCollector = new JmxMessageMetricsCollector();
    metricsCollector.registerListener(this);
    this.setMessageMetricsCollector(metricsCollector);
  }
  
  @Override
  public void init() throws CoreException {
    this.getMessageMetricsCollector().prepare();
    if(this.getPushGateway() == null) {
      if(this.getPrometheusEndpoint() != null)
        this.setPushGateway(new PushGateway(this.getPrometheusEndpoint()));
      else
        log.warn("Prometheus Metrics Adapter could not be started because the bootstrap property or system property {}, was not set.", PROMETHEUS_ENDPOINT_KEY);
    }
  }
  
  @Override
  public void start() throws CoreException {
    if(this.getPushGateway() != null) {
      scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
          return new Thread(runnable, "Prometheus Metric Gatherer");
        }
      });
      
      this.schedulerHandle = this.scheduler.scheduleWithFixedDelay(
          this.getMessageMetricsCollector(), 
          this.collectorIntervalSeconds(), 
          this.collectorIntervalSeconds(), 
          TimeUnit.SECONDS);
    }
  }
  
  @Override
  public void stop() {
    if(this.getPushGateway() != null) {
      if (schedulerHandle != null) {
        this.schedulerHandle.cancel(true);
        scheduler.shutdownNow();
      }
    }
  }
  
  @Override
  public void close() {  }
  
  @Override
  public String getImplementationName() {
    return IMPLEMENTATION_NAME;
  }
  
  private String getPrometheusEndpoint() {
    if(System.getProperty(PROMETHEUS_ENDPOINT_KEY) != null)
      return System.getProperty(PROMETHEUS_ENDPOINT_KEY);
    if(this.getBootstrapProperties().getProperty(PROMETHEUS_ENDPOINT_KEY) != null)
      return this.getBootstrapProperties().getProperty(PROMETHEUS_ENDPOINT_KEY);
    
    return null;
  }
  
  protected int collectorIntervalSeconds() {
    return this.getCollectorIntervalSeconds() == null ? METRICS_COLLECTOR_INTERVAL_SECONDS_DEFAULT : this.getCollectorIntervalSeconds();
  }

  @Override
  public void notifyMessageMetrics(List<MessageStatisticExtended> statistics) {
    CollectorRegistry registry = new CollectorRegistry();
    statistics.forEach(statistic -> {
      Counter msgPerSecondCounter = 
          Counter
          .build()
          .name(statistic.getStatisticId().replace("-", ""))
          .help("Messages per second for the workflow interceptor named " + statistic.getStatisticId().replace("-", ""))
          .register(registry);
      
      long messagesPerSecond = MessagesPerSecondCalculator.calculateMessagesPerSecond(METRICS_COLLECTOR_INTERVAL_SECONDS_DEFAULT, statistic);
      if(messagesPerSecond >= 0) {
        msgPerSecondCounter.inc(messagesPerSecond);
      
        log.trace("Pushing metric '{}' with value '{}' to Prometheus.", statistic.getStatisticId().replace("-", ""), messagesPerSecond);
        
        try {
          this.getPushGateway().pushAdd(registry, PROMETHEUS_JOB_NAME);
        } catch (IOException e) {
          log.warn("Could not push to Prometheus.", e);
        }
      }
    });
  }
  
}
