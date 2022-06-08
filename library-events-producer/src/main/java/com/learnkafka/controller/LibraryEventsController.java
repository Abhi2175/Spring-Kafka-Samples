package com.learnkafka.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.learnkafka.domain.LibraryEvent;
import com.learnkafka.domain.LibraryEventType;
import com.learnkafka.producer.LibraryEventProducer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class LibraryEventsController {

  @Autowired LibraryEventProducer libraryEventProducer;

  @PostMapping("/v1/libraryevent")
  public ResponseEntity<LibraryEvent> postLibraryEvent(
      @RequestBody @Valid LibraryEvent libraryEvent)
      throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {

    // invoke kafka producer asynchronously
    // libraryEventProducer.sendLibraryEvent(libraryEvent);

    // invoke kafka producer synchronously
    // SendResult<Integer, String> sendResult =
    // libraryEventProducer.sendLibraryEventSynchronous(libraryEvent);

    // invoke kafka producer using producerRecords
    libraryEvent.setLibraryEventType(LibraryEventType.NEW);
    libraryEventProducer.sendLibraryEventwithProducerRecord(libraryEvent);
    return ResponseEntity.status(HttpStatus.CREATED).body(libraryEvent);
  }

  // PUT
  @PutMapping("/v1/libraryevent")
  public ResponseEntity<?> putLibraryEvent(@RequestBody @Valid LibraryEvent libraryEvent)
      throws JsonProcessingException {

    if (libraryEvent.getLibraryEventId() == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please pass the libraryEventID");
    }
    libraryEvent.setLibraryEventType(LibraryEventType.UPDATE);
    libraryEventProducer.sendLibraryEventwithProducerRecord(libraryEvent);
    return ResponseEntity.status(HttpStatus.CREATED).body(libraryEvent);
  }
}
