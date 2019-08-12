package com.adaptris.kubernetes.metrics.prometheus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.adaptris.core.interceptor.MessageStatistic;

public class MessagesPerSecondCalculatorTest {
  
  private static final long FIVE_SECONDS = 5000;
  private static final long TWO_SECONDS = 2000;
  
  private MessagesPerSecondCalculator calculator;
  
  @BeforeEach
  public void setUp() throws Exception {
    calculator = new MessagesPerSecondCalculator();
  }
  
  @Test
  public void testNoStats() throws Exception {
    // If there are no stats from JMX, then we should return -1, therefore send nothing to Prometheus.
    long messagesPerSecond = calculator.calculateMessagesPerSecond(10l, new MessageStatisticExtended());
    
    assertEquals(-1, messagesPerSecond);
  }
  
  @Test
  public void testNoCompletedTimeSlices() throws Exception {
    // If there are no completed time slice stats from JMX, then we should return -1, therefore send nothing to Prometheus.
    
    MessageStatisticExtended messageStatisticExtended = new MessageStatisticExtended();
    MessageStatistic uncompletedStat = new MessageStatistic(System.currentTimeMillis() + FIVE_SECONDS);
    messageStatisticExtended.getStatistics().add(uncompletedStat);
    
    long messagesPerSecond = calculator.calculateMessagesPerSecond(10l, messageStatisticExtended);
    
    assertEquals(-1, messagesPerSecond);
  }
  
  @Test
  public void testSingleCompletedTimeSlices() throws Exception {
    // Adapter probably only just started and has processed all of the messages on a queue, lets say 10.
    // No new messages for the last few seconds.
    // We should send 10 to Prometheus, cos the time slice has completed.
    
    MessageStatisticExtended messageStatisticExtended = new MessageStatisticExtended();
    messageStatisticExtended.setStatisticId("MyStatId");
    // This time slice ended 5 seconds ago.
    MessageStatistic completedStat = new MessageStatistic(System.currentTimeMillis() - FIVE_SECONDS);
    completedStat.setTotalMessageCount(10);
    messageStatisticExtended.getStatistics().add(completedStat);
    
    long messagesPerSecond = calculator.calculateMessagesPerSecond(10l, messageStatisticExtended);
    
    assertEquals(10, messagesPerSecond);
  }
  
//  @Test
//  public void testAlreadyProcessedTimeSlices() throws Exception {
//    // Adapter has processed say 10 messages, in a time slice that has ended a while ago.  We'll run the calculator.
//    // Then the adapter processes a further 12 messages, a few seconds later we re-run the calculator.
//    // We expect to get the result 10, followed by 12.
//    
//    MessageStatisticExtended messageStatisticExtended = new MessageStatisticExtended();
//    messageStatisticExtended.setStatisticId("MyStatId");
//    // This time slice ended 15 seconds ago.
//    MessageStatistic completedStat = new MessageStatistic(System.currentTimeMillis() - (FIVE_SECONDS * 3));
//    completedStat.setTotalMessageCount(10);
//    messageStatisticExtended.getStatistics().add(completedStat);
//        
//    assertEquals(10, calculator.calculateMessagesPerSecond(10l, messageStatisticExtended));
//    
//    // now we add a new time slice with 12 messages in it, that ended 5 seconds ago.
//    
//    MessageStatistic secondCompletedStat = new MessageStatistic(System.currentTimeMillis() - (FIVE_SECONDS));
//    secondCompletedStat.setTotalMessageCount(12);
//    messageStatisticExtended.getStatistics().add(secondCompletedStat);
//        
//    assertEquals(12, calculator.calculateMessagesPerSecond(10l, messageStatisticExtended));
//  }
//  
//  @Test
//  public void testAlreadyProcessedWithAnUncompletedTimeSlice() throws Exception {
//    // Similar to the above...
//    // Adapter has processed say 10 messages, in a time slice that has ended a while ago.  We'll run the calculator.
//    // Then the adapter processes a further 12 messages, a few seconds later we re-run the calculator.
//    // A new timeslice has been created but hasn't ended yet and has 6 procesed messages in it.
//    // We expect to return 10, then 12 and because our timeslice has not finished yet -1
//    // -1 Because we are not yet ready to send this metric (it hasn't finished).
//    
//    MessageStatisticExtended messageStatisticExtended = new MessageStatisticExtended();
//    messageStatisticExtended.setStatisticId("MyStatId");
//    // This time slice ended 15 seconds ago.
//    MessageStatistic completedStat = new MessageStatistic(System.currentTimeMillis() - (FIVE_SECONDS * 3));
//    completedStat.setTotalMessageCount(10);
//    messageStatisticExtended.getStatistics().add(completedStat);
//        
//    assertEquals(10, calculator.calculateMessagesPerSecond(10l, messageStatisticExtended));
//    
//    // now we add a new time slice with 12 messages in it, that ended 5 seconds ago.
//    
//    MessageStatistic secondCompletedStat = new MessageStatistic(System.currentTimeMillis() - (FIVE_SECONDS));
//    secondCompletedStat.setTotalMessageCount(12);
//    messageStatisticExtended.getStatistics().add(secondCompletedStat);
//        
//    assertEquals(12, calculator.calculateMessagesPerSecond(10l, messageStatisticExtended));
//    
//    MessageStatistic unCompletedStat = new MessageStatistic(System.currentTimeMillis() + (FIVE_SECONDS));
//    unCompletedStat.setTotalMessageCount(6);
//    messageStatisticExtended.getStatistics().add(unCompletedStat);
//        
//    assertEquals(-1, calculator.calculateMessagesPerSecond(10l, messageStatisticExtended));
//  }
//  
//  @Test
//  public void testAlreadyProcessedWithNoNewMessagesProcessed() throws Exception {
//    // Similar to the above...
//    // Adapter has processed say 10 messages, in a time slice that has ended a while ago.  We'll run the calculator.
//    // Then the adapter processes a further 12 messages, a few seconds later we re-run the calculator.
//    // But for the last 10 seconds no new messages have been processed, we need to reset the Prometheus stat to say we haven't processed any in a while, so zero returned.
//    
//    MessageStatisticExtended messageStatisticExtended = new MessageStatisticExtended();
//    messageStatisticExtended.setStatisticId("MyStatId");
//    // This time slice ended 15 seconds ago.
//    MessageStatistic completedStat = new MessageStatistic(System.currentTimeMillis() - (FIVE_SECONDS * 3));
//    completedStat.setTotalMessageCount(10);
//    messageStatisticExtended.getStatistics().add(completedStat);
//        
//    assertEquals(10, calculator.calculateMessagesPerSecond(10l, messageStatisticExtended));
//    
//    // now we add a new time slice with 12 messages in it, that ended 10 seconds ago.
//    MessageStatistic secondCompletedStat = new MessageStatistic(System.currentTimeMillis() - (FIVE_SECONDS * 2));
//    secondCompletedStat.setTotalMessageCount(12);
//    messageStatisticExtended.getStatistics().add(secondCompletedStat);
//        
//    assertEquals(12, calculator.calculateMessagesPerSecond(10l, messageStatisticExtended));
//
//    // You'll notice we have not created a new time slice with zero messages in it.  
//    // Simply because new time slices are only created when we process a message!  So there will
//    // never be a time slice with zero messages in it.
//    // The calculator runs and should return zero, because it has already processed the above two time slices
//    // and no new messages since (we know that because there is no active (uncompleted) timeslice).
//    assertEquals(0, calculator.calculateMessagesPerSecond(10l, messageStatisticExtended));
//  }
//  
//  @Test
//  public void testAlreadyProcessedWithNoNewMessagesProcessedMultipleCalInvocations() throws Exception {
//    // Almost identical to the above...
//    // This is for extra bonus points.  If we send zero to prometheus once, then we shouldn't continue to send zero time after time.
//    // We can save network IO by simply returning -1 instead of a second/third/millionth zero in a row.
//    // Remember if we return -1 from this method then nothing is sent to Prometheus.
//    
//    MessageStatisticExtended messageStatisticExtended = new MessageStatisticExtended();
//    messageStatisticExtended.setStatisticId("MyStatId");
//    // This time slice ended 15 seconds ago.
//    MessageStatistic completedStat = new MessageStatistic(System.currentTimeMillis() - (FIVE_SECONDS * 3));
//    completedStat.setTotalMessageCount(10);
//    messageStatisticExtended.getStatistics().add(completedStat);
//        
//    assertEquals(10, calculator.calculateMessagesPerSecond(10l, messageStatisticExtended));
//    
//    // now we add a new time slice with 12 messages in it, that ended 10 seconds ago.
//    MessageStatistic secondCompletedStat = new MessageStatistic(System.currentTimeMillis() - (FIVE_SECONDS * 2));
//    secondCompletedStat.setTotalMessageCount(12);
//    messageStatisticExtended.getStatistics().add(secondCompletedStat);
//        
//    assertEquals(12, calculator.calculateMessagesPerSecond(10l, messageStatisticExtended));
//
//    // You'll notice we have not created a new time slice with zero messages in it.  
//    // Simply because new time slices are only created when we process a message!  So there will
//    // never be a time slice with zero messages in it.
//    // The calculator runs and should return zero, because it has already processed the above two time slices and sent the values to Prometheus
//    // and no new messages since (we know that because there is no active (uncompleted) timeslice).
//    assertEquals(0, calculator.calculateMessagesPerSecond(10l, messageStatisticExtended));
//    
//    // We've sent zero previously, which is good because it resets the Prometheus stat.
//    // but now we still haven't processed any messages, so lets not send unnecessary IO and just return -1.
//    assertEquals(-1, calculator.calculateMessagesPerSecond(10l, messageStatisticExtended));
//  }
  
  @Test
  public void testMultipleUnprocessedTimeslices() throws Exception {
    // The calculator is called every 10 seconds.  But what if the user has configured their 
    // Intercepter to have time slices of 2 seconds.
    // And lets say they have 2 completed times slices (with 15 and 20 messages processed) that we have not calculated yet.
    
    MessageStatisticExtended messageStatisticExtended = new MessageStatisticExtended();
    messageStatisticExtended.setStatisticId("MyStatId");
    
    // This time slice ended 6 seconds ago.
    MessageStatistic completedStat = new MessageStatistic(System.currentTimeMillis() - (TWO_SECONDS * 3));
    completedStat.setTotalMessageCount(15);
    messageStatisticExtended.getStatistics().add(completedStat);
            
    // now we add a new time slice with 20 messages in it, that ended 4 seconds ago.
    MessageStatistic secondCompletedStat = new MessageStatistic(System.currentTimeMillis() - (TWO_SECONDS * 2));
    secondCompletedStat.setTotalMessageCount(20);
    messageStatisticExtended.getStatistics().add(secondCompletedStat);
        
    // We expect that because this methid is called every 10 seconds, we should tally up all unprocessed
    // and completed time slices into a single value.  So 15 + 20, we should return 35.
    assertEquals(35, calculator.calculateMessagesPerSecond(10l, messageStatisticExtended));

  }
  

}
