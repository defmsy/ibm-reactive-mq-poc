package com.example.reactivemq.controllers;

import com.example.reactivemq.entities.Message;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(
    value = "/mq",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
public class MqController {

  public static final Logger log = LoggerFactory.getLogger(MqController.class);
  public static final String QUEUE_NAME = "DEV.QUEUE.1";

  private final JmsTemplate jmsTemplate;

  public MqController(JmsTemplate jmsTemplate) {
    this.jmsTemplate = jmsTemplate;
  }

  @PutMapping("/sendandreceive")
  public Mono<String> sendAndReceive(@RequestBody String message) {
    String correlationId = UUID.randomUUID().toString();
    return publish(message, correlationId)
        .flatMap(result -> consume(correlationId));
  }

  @PostMapping("/sendandreceive")
  public Flux<Message> sendAndReceiveMany(@RequestBody List<String> messages) {
    return Flux.fromIterable(messages)
        .flatMap(message -> Mono.just(message)
            .zipWith(this.generateCorrelationId())
            .flatMap(tuple -> this.publish(tuple.getT1(), tuple.getT2()))
            .flatMap(this::consume)
            .map(Message::new)
        );
  }

  @PutMapping("/send/{correlationId}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Mono<String> send(@RequestBody String message, @PathVariable String correlationId) {
    return publish(message, correlationId);
  }

  @GetMapping("/receive/{correlationId}")
  public Mono<String> receive(@PathVariable String correlationId) {
    return consume(correlationId);
  }

  private Mono<String> publish(String message, String correlationID) {
    return Mono.fromCallable(() -> {
      this.jmsTemplate.convertAndSend(QUEUE_NAME, message, m -> {
        m.setJMSCorrelationID(correlationID);
        return m;
      });
      return correlationID;
    }).doOnError(throwable -> log.warn("Fail to send message", throwable));
  }

  private Mono<String> consume(String correlationID) {
    var filter = String.format("JMSCorrelationID='%s'", correlationID);
    return Mono.fromCallable(() -> {
          var result = Optional.ofNullable(
              this.jmsTemplate.receiveSelectedAndConvert(QUEUE_NAME, filter));
          return result.orElse("NOT_FOUND").toString();
        })
        .doOnError(throwable -> log.warn("Fail to send message", throwable));
  }

  private Mono<String> generateCorrelationId() {
    return Mono.just(UUID.randomUUID().toString());
  }

}
