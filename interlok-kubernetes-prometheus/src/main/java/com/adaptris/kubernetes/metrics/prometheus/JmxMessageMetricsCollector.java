package com.adaptris.kubernetes.metrics.prometheus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import com.adaptris.core.CoreException;
import com.adaptris.core.interceptor.MessageMetricsStatistics;
import com.adaptris.core.interceptor.MessageMetricsStatisticsMBean;
import com.adaptris.core.util.JmxHelper;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JmxMessageMetricsCollector implements MessageMetricsCollector {
   
  private static final String METRICS_OBJECT_NAME = "com.adaptris:type=Metrics,*";
  
  private static final String ADAPTER_PROPERTY = "adapter";
  
  private static final String CHANNEL_PROPERTY = "channel";
  
  private static final String WORKFLOW_PROPERTY = "workflow";
  
  private static final String ID_PROPERTY = "id";
  
  
  @Getter
  @Setter
  private MBeanServer interlokMBeanServer;
  
  @Getter
  @Setter
  private Map<ObjectName, MessageMetricsStatisticsMBean> metricsMBeans;
  
  @Getter
  @Setter
  private List<MessageMetricsListener> listeners;
  
  public JmxMessageMetricsCollector() {
    this.setListeners(new ArrayList<>());
  }
  
  @Override
  public void run() {
    List<MessageStatisticExtended> foundStatistics = new ArrayList<MessageStatisticExtended>();
    try {
      if(this.getInterlokMBeanServer() == null)
        this.setInterlokMBeanServer(JmxHelper.findMBeanServer());
      
      if(this.getMetricsMBeans() == null)
        this.reloadMBeans();
    
      this.getMetricsMBeans().forEach((objectName,messageMetricsStats) -> {
        MessageStatisticExtended messageStatisticExtended = new MessageStatisticExtended();
        try {
          messageStatisticExtended.getStatistics().addAll(messageMetricsStats.getStatistics());
        } catch (CoreException e) {}
        
        messageStatisticExtended.setAdapterId(objectName.getKeyProperty(ADAPTER_PROPERTY));
        messageStatisticExtended.setChannelId(objectName.getKeyProperty(CHANNEL_PROPERTY));
        messageStatisticExtended.setWorkflowId(objectName.getKeyProperty(WORKFLOW_PROPERTY));
        messageStatisticExtended.setStatisticId(objectName.getKeyProperty(ID_PROPERTY));
        
        foundStatistics.add(messageStatisticExtended);
      });
      
      this.notifyListeners(foundStatistics);
      
    } catch (Exception ex) {
      log.warn("Error collecting message metrics from JMX, continuing...", ex);
    }
    
  }

  private void reloadMBeans() throws Exception {
    this.setMetricsMBeans(new HashMap<>());
    
    if(this.getInterlokMBeanServer() == null)
      this.setInterlokMBeanServer(JmxHelper.findMBeanServer());
    
    Set<ObjectInstance> mBeans = this.getInterlokMBeanServer().queryMBeans(new ObjectName(METRICS_OBJECT_NAME), null);
    for(ObjectInstance instance : mBeans) {
      if(instance.getClassName().equals(MessageMetricsStatistics.class.getName())) {
        log.trace("Found and caching metrics mbean: {}", instance.getObjectName().toString());
        this.getMetricsMBeans().put(instance.getObjectName(), JMX.newMBeanProxy(getInterlokMBeanServer(), instance.getObjectName(), MessageMetricsStatisticsMBean.class));
      }
    }
    
  }

  @Override
  public void prepare() throws CoreException {
    // Incase someone restarts the adapter after adding new mbeans
    // Let's reset everything
    this.setInterlokMBeanServer(null);
    this.setMetricsMBeans(null);
  }

  @Override
  public void registerListener(MessageMetricsListener listener) {
    this.getListeners().add(listener);
  }

  @Override
  public void deregisterListener(MessageMetricsListener listener) {
    this.getListeners().remove(listener);
  }

  @Override
  public void notifyListeners(List<MessageStatisticExtended> stats) {
    for(MessageMetricsListener listener : this.getListeners())
      listener.notifyMessageMetrics(stats);
  }

}
