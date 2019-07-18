package com.adaptris.mgmt.kubernetes.metrics;

import java.util.Properties;
import java.util.ServiceLoader;

import com.adaptris.core.CoreException;
import com.adaptris.core.management.ManagementComponent;
import com.adaptris.core.util.LifecycleHelper;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KubernetesMetricsComponent implements ManagementComponent {
  
  @Getter
  @Setter
  private ServiceLoader<KubernetesMetricsAdapter> metricsAdapters;

  public KubernetesMetricsComponent() {
    this.setMetricsAdapters(ServiceLoader.load(KubernetesMetricsAdapter.class));
  }
  
  @Override
  public void init(@NonNull Properties config) throws Exception {
    this.getMetricsAdapters().forEach(e -> {
      e.setBootstrapProperties(config);
      try {
        LifecycleHelper.init(e);
        log.info("Found and initializing metrics adapter {}", e.getImplementationName());
      } catch (CoreException e1) {
        log.error("Could not initialize metrics adapter {}", e.getImplementationName(), e1);
      }
    });
  }

  @Override
  public void start() throws Exception {
    this.getMetricsAdapters().forEach(e -> {
      try {
        LifecycleHelper.start(e);
        log.info("Starting metrics adapter {}", e.getImplementationName());
      } catch (CoreException e1) {
        log.error("Could not start metrics adapter {}", e.getImplementationName(), e1);
      }
    });
  }

  @Override
  public void stop() throws Exception {
    this.getMetricsAdapters().forEach(e -> {
      LifecycleHelper.stop(e);
      log.info("Stopping metrics adapter {}", e.getImplementationName());
    });
  }

  @Override
  public void destroy() throws Exception {
    this.getMetricsAdapters().forEach(e -> {
      LifecycleHelper.close(e);
      log.info("Destroying metrics adapter {}", e.getImplementationName());
    });
  }
  
}
