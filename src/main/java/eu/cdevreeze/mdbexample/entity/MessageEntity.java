/*
 * Copyright 2025-2025 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.mdbexample.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Message JPA entity.
 *
 * @author Chris de Vreeze
 */
@Entity(name = "Message")
@Table(name = "message")
public class MessageEntity {

    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    @Column(name = "id")
    private long id;

    // See https://www.baeldung.com/java-postgresql-store-date-time

    @Column(name = "creation_time")
    private LocalDateTime timestamp;

    @Column(name = "message_text")
    private String messageText;

    public MessageEntity() {
    }

    public MessageEntity(long id, LocalDateTime timestamp, String messageText) {
        this.id = id;
        this.timestamp = timestamp;
        this.messageText = messageText;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageEntity message)) return false;
        return id == message.id && Objects.equals(timestamp, message.timestamp) && Objects.equals(messageText, message.messageText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, messageText);
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", messageText='" + messageText + '\'' +
                '}';
    }
}
