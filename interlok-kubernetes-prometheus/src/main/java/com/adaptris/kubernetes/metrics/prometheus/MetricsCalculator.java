package com.adaptris.kubernetes.metrics.prometheus;

public interface MetricsCalculator {

  public long calculateMessagesPerSecond(long calculateForTheLastNumberOfSeconds, MessageStatisticExtended statistic);
  
}
