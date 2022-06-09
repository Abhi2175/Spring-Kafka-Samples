package com.learnkafka.intg;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.learnkafka.consumer.LibraryEventConsumer;
import com.learnkafka.service.LibraryEventsService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EmbeddedKafka(
    topics = {"library-events"},
    partitions = 3)
@TestPropertySource(
    properties = {
      "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
      "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"
    })
public class LibraryEventsConsumerIntegrationTest {

  @Autowired EmbeddedKafkaBroker embeddedKafkaBroker;

  @Autowired KafkaTemplate<Integer, String> kafkaTemplate;

  @Autowired KafkaListenerEndpointRegistry endpointRegistry;

  @SpyBean LibraryEventConsumer libraryEventsConsumerSpy;

  @SpyBean LibraryEventsService libraryEventsServiceSpy;

  @BeforeEach
  void setUp() {

    for (MessageListenerContainer messageListenerContainer :
        endpointRegistry.getListenerContainers()) {
      ContainerTestUtils.waitForAssignment(
          messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());
    }
  }

  @Test
  void publishNewLibraryEvent()
      throws ExecutionException, InterruptedException, JsonProcessingException {

    // given
    String json =
        " {\"libraryEventId\":null,\"libraryEventType\":\"NEW\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka Using Spring Boot\",\"bookAuthor\":\"Dilip\"}}";
    kafkaTemplate.sendDefault(json).get();

    // when
    CountDownLatch latch = new CountDownLatch(1);
    latch.await(3, TimeUnit.SECONDS);

    // then
    verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
    verify(libraryEventsServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));
  }
}
