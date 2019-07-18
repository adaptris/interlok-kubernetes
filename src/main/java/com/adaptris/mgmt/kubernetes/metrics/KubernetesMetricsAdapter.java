package com.adaptris.mgmt.kubernetes.metrics;

import java.util.Properties;

import com.adaptris.core.ComponentLifecycle;

public interface KubernetesMetricsAdapter extends ComponentLifecycle {

  /**
   * Return the name of the metrics adapter this implementation is built for.
   * An example might be "prometheus", "stackdriver" etc.
   * @return String
   */
  public String getImplementationName();
  
  /**
   * Allow the metrics adapters to configure themselves through the Bootstrap Properties.
   * @param bootstrapProperties
   */
  public void setBootstrapProperties(Properties bootstrapProperties);
  
  
}
