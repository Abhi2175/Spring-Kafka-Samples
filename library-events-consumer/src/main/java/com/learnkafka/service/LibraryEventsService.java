package com.learnkafka.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.entity.LibraryEvent;
import com.learnkafka.jpa.LibraryEventsRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LibraryEventsService {

  @Autowired ObjectMapper objectMapper;

  @Autowired private LibraryEventsRepository libraryEventsRepository;

  public void processLibraryEvent(ConsumerRecord<Integer, String> consumerRecord)
      throws JsonProcessingException {

    LibraryEvent libraryEvent = objectMapper.readValue(consumerRecord.value(), LibraryEvent.class);
    log.info("LibraryEvent: {}", libraryEvent);

    if ((libraryEvent != null
        && libraryEvent.getLibraryEventId() != null
        && libraryEvent.getLibraryEventId() == 999))
      throw new RecoverableDataAccessException("Temporary Network Issue");

    switch (libraryEvent.getLibraryEventType()) {
      case NEW:
        save(libraryEvent);
        break;
      case UPDATE:
        validate(libraryEvent);
        save(libraryEvent);
        break;
      default:
        log.info("Invalid Library Event Type");
    }
  }

  private void validate(LibraryEvent libraryEvent) {
    if (libraryEvent.getLibraryEventId() == null) {
      throw new IllegalArgumentException("LibraryEventID is missing");
    }

    Optional<LibraryEvent> libraryEventOptional =
        libraryEventsRepository.findById(libraryEvent.getLibraryEventId());
    if (libraryEventOptional.isEmpty()) {
      throw new IllegalArgumentException("Not a valid Library Event");
    }
    log.info("Validation successful for the library event: {}", libraryEventOptional.get());
  }

  private void save(LibraryEvent libraryEvent) {

    libraryEvent.getBook().setLibraryEvent(libraryEvent);
    libraryEventsRepository.save(libraryEvent);
    log.info("Successfully persisted the libraryEvent: {}", libraryEvent);
  }
}
