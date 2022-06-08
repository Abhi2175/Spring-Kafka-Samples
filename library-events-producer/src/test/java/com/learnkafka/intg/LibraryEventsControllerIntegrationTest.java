package com.learnkafka.intg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.learnkafka.domain.Book;
import com.learnkafka.domain.LibraryEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LibraryEventsControllerIntegrationTest {

  @Autowired TestRestTemplate restTemplate;

  @Test
  void postLibraryEvent() {
    // given
    Book book =
        Book.builder()
            .bookId(123)
            .bookAuthor("Abhishek")
            .bookName("Kafka using Spring Boot")
            .build();

    LibraryEvent libraryEvent = LibraryEvent.builder().libraryEventId(null).book(book).build();
    HttpHeaders headers = new HttpHeaders();
    headers.set("content-type", MediaType.APPLICATION_JSON.toString());
    HttpEntity<LibraryEvent> request = new HttpEntity<>(libraryEvent, headers);

    // when
    ResponseEntity<LibraryEvent> responseEntity =
        restTemplate.exchange("/v1/libraryevent", HttpMethod.POST, request, LibraryEvent.class);

    // then
    assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
  }
}
