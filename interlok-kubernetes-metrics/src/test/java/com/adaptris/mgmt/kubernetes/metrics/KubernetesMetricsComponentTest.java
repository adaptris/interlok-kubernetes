package com.adaptris.mgmt.kubernetes.metrics;

import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class KubernetesMetricsComponentTest {

  private KubernetesMetricsComponent component;
  
  @BeforeEach
  public void setUp() throws Exception {
    component = new KubernetesMetricsComponent();
  }
  
  @AfterEach
  public void tearDown() throws Exception {
    
  }
  
  @Test
  public void testNoImplementationsNoError() throws Exception {
    component.init(new Properties());
    component.start();
    
    component.stop();
    component.destroy();
  }
  
}
