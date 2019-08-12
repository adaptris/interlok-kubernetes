package com.adaptris.kubernetes.metrics.prometheus;

import java.util.ArrayList;
import java.util.List;

import com.adaptris.core.interceptor.MessageStatistic;

import lombok.Getter;
import lombok.Setter;

public class MessageStatisticExtended {

  @Getter
  @Setter
  private List<MessageStatistic> statistics;
  
  @Getter
  @Setter
  private String adapterId;
  
  @Getter
  @Setter
  private String channelId;
  
  @Getter
  @Setter
  private String workflowId;
  
  @Getter
  @Setter
  private String statisticId;
  
  public MessageStatisticExtended() {
    this.setStatistics(new ArrayList<MessageStatistic>());
  }
  
}
