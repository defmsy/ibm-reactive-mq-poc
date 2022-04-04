package com.example.reactivemq;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.reactivemq.entities.Message;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootTest
@AutoConfigureWebTestClient
class MqControllerTest {

  public static final String HELLO_WORLD = "Hello world!";
  public static final String CORRELATION_ID = "CORRELATION_ID";
  @Autowired
  private WebTestClient webTestClient;

  @Test
  @Order(1)
  void shouldPublishMessage() {
    this.sendMessage(HELLO_WORLD, CORRELATION_ID);
  }

  @Test
  @Order(2)
  void shouldReturnMessage() {
    this.consumeMessage(HELLO_WORLD, CORRELATION_ID);
  }

  @Test
  @Order(3)
  void shouldSendAndReturnMessage() {
    this.webTestClient
        .put()
        .uri("/mq/sendandreceive")
        .body(Mono.just(HELLO_WORLD), String.class)
        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class).isEqualTo(HELLO_WORLD);
  }

  @Test
  @Order(4)
  void shouldReturnTheRightMessage() {
    String firstMessage = "First message";
    String secondMessage = "Second message";
    String thirdMessage = "Third message";
    String firstCorrelationId = UUID.randomUUID().toString();
    String secondCorrelationId = UUID.randomUUID().toString();
    String thirdCorrelationId = UUID.randomUUID().toString();

    this.sendMessage(firstMessage, firstCorrelationId);
    this.sendMessage(secondMessage, secondCorrelationId);
    this.sendMessage(thirdMessage, thirdCorrelationId);
    this.consumeMessage(secondMessage, secondCorrelationId);
    this.consumeMessage(thirdMessage, thirdCorrelationId);
    this.consumeMessage(firstMessage, firstCorrelationId);
  }

  @Test
  @Order(5)
  void shouldSendAndReceiveMany() {
    var messages = List.of(
        "Pomme",
        "Poire",
        "Banane",
        "Orange",
        "Citron",
        "Goyave"
    );
    var result = this.webTestClient
        .post()
        .uri("/mq/sendandreceive")
        .body(Flux.fromIterable(messages), List.class)
        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(Message.class)
        .hasSize(6)
        .returnResult();

    assertThat(result.getResponseBody())
        .containsExactlyInAnyOrderElementsOf(
            messages.stream().map(Message::new).collect(Collectors.toList()));
  }

  private void sendMessage(String message, String correlationId) {
    this.webTestClient
        .put()
        .uri("/mq/send/{correlationId}", correlationId)
        .body(Mono.just(message), String.class)
        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.ACCEPTED)
        .expectBody(String.class).isEqualTo(correlationId);
  }

  private void consumeMessage(String expectedMessage, String correlationId) {
    this.webTestClient
        .get()
        .uri("/mq/receive/{correlationId}", correlationId)
        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class).isEqualTo(expectedMessage);
  }

}
