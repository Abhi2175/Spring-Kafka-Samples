package com.learnkafka.schedular;

import static com.learnkafka.config.LibraryEventConsumerConfig.RETRY;
import static com.learnkafka.config.LibraryEventConsumerConfig.SUCCESS;

import com.learnkafka.entity.FailureRecord;
import com.learnkafka.jpa.FailureRecordRepository;
import com.learnkafka.service.LibraryEventsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RetrySchedular {

  private FailureRecordRepository failureRecordRepository;
  private LibraryEventsService libraryEventsService;

  public RetrySchedular(
      FailureRecordRepository failureRecordRepository, LibraryEventsService libraryEventsService) {
    this.failureRecordRepository = failureRecordRepository;
    this.libraryEventsService = libraryEventsService;
  }

  @Scheduled(fixedRate = 10000)
  public void retryFailedRecords() {

    log.info("Retrying Failed Record Started");
    failureRecordRepository
        .findAllByStatus(RETRY)
        .forEach(
            failureRecord -> {
              log.info("Retrying Failed Record : {}", failureRecord);
              var consumerRecord = buildConsumerRecord(failureRecord);
              try {
                libraryEventsService.processLibraryEvent(consumerRecord);
                failureRecord.setStatus(SUCCESS);
              } catch (Exception e) {
                log.error("Exception in retryFailedRecords : {}", e.getMessage(), e);
              }
            });
    log.info("Retrying Failed Record Completed");
  }

  private ConsumerRecord<Integer, String> buildConsumerRecord(FailureRecord failureRecord) {

    return new ConsumerRecord<>(
        failureRecord.getTopic(),
        failureRecord.getPartition(),
        failureRecord.getOffset_value(),
        failureRecord.getKey(),
        failureRecord.getErrorRecord());
  }
}
