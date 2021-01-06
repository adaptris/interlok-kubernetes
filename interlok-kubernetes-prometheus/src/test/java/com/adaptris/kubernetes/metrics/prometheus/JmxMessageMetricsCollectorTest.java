package com.adaptris.kubernetes.metrics.prometheus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adaptris.core.interceptor.MessageMetricsStatistics;
import com.adaptris.core.interceptor.MessageMetricsStatisticsMBean;
import com.adaptris.core.interceptor.MessageStatistic;

public class JmxMessageMetricsCollectorTest {

  private JmxMessageMetricsCollector collector;

  @Mock private MBeanServer mockMBeanServer;

  private Set<ObjectInstance> objectSet;

  @Mock private ObjectInstance mockObjectInstance;

  @Mock private ObjectName mockObjectName;

  private ObjectName realObjectName;

  @Mock private MessageMetricsStatisticsMBean mockMMSMBean;

  private List<MessageStatistic> messagesStatisticsList;

  @Mock MessageMetricsListener mockListener;

  private AutoCloseable closeable;

  @BeforeEach
  public void setUp() throws Exception {
    closeable = MockitoAnnotations.openMocks(this);

    collector = new JmxMessageMetricsCollector();
    collector.setInterlokMBeanServer(mockMBeanServer);

    objectSet = new HashSet<>();
    objectSet.add(mockObjectInstance);

    realObjectName = new ObjectName("com.adaptris:type=Metrics,adapter=adapter,channel=channel,workflow=workflow");

    messagesStatisticsList = new ArrayList<>();

    when(mockMBeanServer.queryMBeans(any(ObjectName.class), any(QueryExp.class)))
    .thenReturn(objectSet);

    when(mockMMSMBean.getStatistics())
    .thenReturn(messagesStatisticsList);
  }

  @AfterEach
  public void tearDown() throws Exception {
    closeable.close();
  }

  @Test
  public void testMBeansLoadedFirstExecution() {
    when(mockObjectInstance.getClassName()).thenReturn(MessageMetricsStatistics.class.getName());
    when(mockObjectInstance.getObjectName()).thenReturn(mockObjectName);

    try {
      collector.run();
    } catch (Throwable t) {};

    verify(mockMBeanServer).queryMBeans(any(ObjectName.class), isNull());
  }

  @Test
  public void testNotifyNewStats() throws Exception {
    Map<ObjectName, MessageMetricsStatisticsMBean> mbeans = new HashMap<>();
    mbeans.put(realObjectName, mockMMSMBean);

    collector.registerListener(mockListener);

    collector.setMetricsMBeans(mbeans);
    collector.run();

    collector.deregisterListener(mockListener);

    Thread.sleep(500);

    verify(mockListener).notifyMessageMetrics(any());
  }

}
