package com.adaptris.kubernetes.metrics.prometheus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.adaptris.core.interceptor.MessageStatistic;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessagesPerSecondCalculator {
  
  private static final int MAX_KEYS = 1000;

  private static List<String> statsAlreadyProcessed;
  
  static {
    statsAlreadyProcessed = Collections.synchronizedList(new ArrayList<String> ());
  }
  /**
   * 
   * @param statistics
   * @return
   */
  public static long calculateMessagesPerSecond(long calculateForTheLastNumberOfSeconds, MessageStatisticExtended statistic) {
    long result = -1;
    
    long now = System.currentTimeMillis();
    long amountOfTimeTalliedFor = 0l;
    
    for(int counter = statistic.getStatistics().size() - 1; counter >= 0; counter --) {
      MessageStatistic messageStatistic = statistic.getStatistics().get(counter);
      
      if(messageStatistic.getEndMillis() < now) { // only handle completed time slices
        String statsKey = statistic.getStatisticId() + Long.toString(messageStatistic.getStartMillis()) + Long.toString(messageStatistic.getEndMillis());
        if(!statsAlreadyProcessed.contains(statsKey)) { // only process those we haven't processed before
          if(result == -1) {
            result = messageStatistic.getTotalMessageCount();
            log.trace("Adding {} from timeslice ending {}", messageStatistic.getTotalMessageCount(), messageStatistic.getEndMillis());
          }
          else {
            result += messageStatistic.getTotalMessageCount();
            log.trace("Adding {} from timeslice ending {}", messageStatistic.getTotalMessageCount(), messageStatistic.getEndMillis());
          }
          amountOfTimeTalliedFor += ((messageStatistic.getEndMillis() - messageStatistic.getStartMillis()) / 1000);
          
          addToProcessed(statsKey);
        } else {
          if(counter == 0) // we have already processed the latest time slice and it is completed and there are no new ones, then no new messages have come through.
            result = 0;
          break;  // if we find one we have already processed, assume the others below it have too
        }
        
        if(amountOfTimeTalliedFor > calculateForTheLastNumberOfSeconds)
          break;
      }
    }
    
    return result;
  }
  private static void addToProcessed(String statsKey) {
    if(statsAlreadyProcessed.size() >= MAX_KEYS) {
      statsAlreadyProcessed.remove(0);
    }
    statsAlreadyProcessed.add(statsKey);
  }
  
}
