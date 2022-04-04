package com.example.reactivemq.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.StringJoiner;

@JsonInclude(Include.NON_NULL)
public class Message {

  private final String message;

  @JsonCreator
  public Message(@JsonProperty("message") String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Message)) {
      return false;
    }

    Message message1 = (Message) o;

    return message != null ? message.equals(message1.message) : message1.message == null;
  }

  @Override
  public int hashCode() {
    return message != null ? message.hashCode() : 0;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Message.class.getSimpleName() + "[", "]")
        .add("message='" + message + "'")
        .toString();
  }
}
