package com.adaptris.kubernetes.metrics.prometheus;

import java.util.List;

public interface MessageMetricsListener {

  public void notifyMessageMetrics(List<MessageStatisticExtended> statistics);
  
}
