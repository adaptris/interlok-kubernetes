package com.adaptris.kubernetes.metrics.prometheus;

import java.util.List;

public interface MessageMetricsNotifier {

  public void registerListener(MessageMetricsListener listener);
  
  public void deregisterListener(MessageMetricsListener listener);
  
  public void notifyListeners(List<MessageStatisticExtended> stats);
  
}
