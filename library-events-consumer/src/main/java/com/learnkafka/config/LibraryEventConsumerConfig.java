package com.learnkafka.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
@Slf4j
public class LibraryEventConsumerConfig {

  @Autowired KafkaTemplate kafkaTemplate;

  @Value("${topics.retry}")
  private String retryTopic;

  @Value("${topics.dlt}")
  private String deadLetterTopic;

  public DeadLetterPublishingRecoverer publishingRecoverer() {

    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (r, e) -> {
              if ((e.getCause() instanceof RecoverableDataAccessException)) {
                return new TopicPartition(retryTopic, r.partition());
              } else {
                return new TopicPartition(deadLetterTopic, r.partition());
              }
            });
    return recoverer;
  }

  public DefaultErrorHandler errorHandler() {

    var exceptionToIgnoreList =
        List.of(IllegalArgumentException.class, JsonProcessingException.class);

    var exceptionToRetryList = List.of(RecoverableDataAccessException.class);

    var fixedBackOff = new FixedBackOff(1000L, 2);
    var errorHandler = new DefaultErrorHandler(publishingRecoverer(), fixedBackOff);

    var expBackOff = new ExponentialBackOffWithMaxRetries(2);
    expBackOff.setInitialInterval(1_000L);
    expBackOff.setMultiplier(2.0);
    expBackOff.setMaxInterval(2_000L);

    // var errorHandler = new DefaultErrorHandler(expBackOff);

    errorHandler.addNotRetryableExceptions();
    exceptionToIgnoreList.forEach(errorHandler::addNotRetryableExceptions);
    exceptionToRetryList.forEach(errorHandler::addRetryableExceptions);

    errorHandler.setRetryListeners(
        (((record, ex, deliveryAttempt) -> {
          log.info(
              "Failed record in Retry Listener, Exception : {}, deliveryAttempt: {}",
              ex.getMessage(),
              deliveryAttempt);
        })));

    return errorHandler;
  }

  @Bean
  ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
      ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
      ConsumerFactory<Object, Object> kafkaConsumerFactory) {
    ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    configurer.configure(factory, kafkaConsumerFactory);
    factory.setConcurrency(3);
    factory.setCommonErrorHandler(errorHandler());
    // factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
    return factory;
  }
}
