package com.learnkafka.unit.producer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.domain.Book;
import com.learnkafka.domain.LibraryEvent;
import com.learnkafka.producer.LibraryEventProducer;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

@ExtendWith(MockitoExtension.class)
public class LibraryEventProducerUnitTest {

  @Mock KafkaTemplate<Integer, String> kafkaTemplate;

  @Spy ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks LibraryEventProducer eventProducer;

  @Test
  void sendLibraryEventwithProducerRecord_Failure() {

    Book book =
        Book.builder()
            .bookId(123)
            .bookAuthor("Abhishek")
            .bookName("Kafka using Spring Boot")
            .build();

    LibraryEvent libraryEvent = LibraryEvent.builder().libraryEventId(null).book(book).build();

    SettableListenableFuture future = new SettableListenableFuture();
    future.setException(new RuntimeException("Exception Calling Kafka"));

    when(kafkaTemplate.send(isA(ProducerRecord.class))).thenReturn(future);

    assertThrows(
        Exception.class,
        () -> eventProducer.sendLibraryEventwithProducerRecord(libraryEvent).get());
  }

  @Test
  void sendLibraryEventwithProducerRecord_Success()
      throws JsonProcessingException, ExecutionException, InterruptedException {

    Book book =
        Book.builder()
            .bookId(123)
            .bookAuthor("Abhishek")
            .bookName("Kafka using Spring Boot")
            .build();

    LibraryEvent libraryEvent = LibraryEvent.builder().libraryEventId(null).book(book).build();

    String record = objectMapper.writeValueAsString(libraryEvent);
    SettableListenableFuture future = new SettableListenableFuture();

    ProducerRecord<Integer, String> producerRecord =
        new ProducerRecord("library-events", libraryEvent.getLibraryEventId(), record);
    RecordMetadata recordMetadata =
        new RecordMetadata(
            new TopicPartition("library-events", 1), 1, 1, 342, System.currentTimeMillis(), 1, 2);

    SendResult<Integer, String> sendResult = new SendResult<>(producerRecord, recordMetadata);

    future.set(sendResult);

    when(kafkaTemplate.send(isA(ProducerRecord.class))).thenReturn(future);

    ListenableFuture<SendResult<Integer, String>> listenableFuture =
        eventProducer.sendLibraryEventwithProducerRecord(libraryEvent);

    SendResult<Integer, String> sendResult1 = listenableFuture.get();

    assert (sendResult1.getRecordMetadata().partition() == 1);
  }
}
