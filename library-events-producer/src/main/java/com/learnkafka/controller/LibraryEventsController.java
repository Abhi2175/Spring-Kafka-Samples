package com.learnkafka.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.learnkafka.domain.LibraryEvent;
import com.learnkafka.domain.LibraryEventType;
import com.learnkafka.producer.LibraryEventProducer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class LibraryEventsController {

  @Autowired LibraryEventProducer libraryEventProducer;

  @PostMapping("/v1/libraryevent")
  public ResponseEntity<LibraryEvent> postLibraryEvent(@RequestBody LibraryEvent libraryEvent)
      throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {

    // invoke kafka producer asyncronously
    // libraryEventProducer.sendLibraryEvent(libraryEvent);

    // invoke kafka producer syncronously
    // SendResult<Integer, String> sendResult =
    // libraryEventProducer.sendLibraryEventSynchronous(libraryEvent);

    // invoke kafka producer using producerRecords
    libraryEvent.setLibraryEventType(LibraryEventType.NEW);
    libraryEventProducer.sendLibraryEventwithProducerRecord(libraryEvent);
    return ResponseEntity.status(HttpStatus.CREATED).body(libraryEvent);
  }

  // PUT
}
