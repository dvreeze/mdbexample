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

package eu.cdevreeze.mdbexample.service;

import eu.cdevreeze.mdbexample.dao.MessageDao;
import eu.cdevreeze.mdbexample.entity.MessageEntity;
import eu.cdevreeze.mdbexample.model.Message;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Message service.
 *
 * @author Chris de Vreeze
 */
@Dependent
public class MessageService {

    private final MessageDao messageDao;

    @Inject
    public MessageService(MessageDao messageDao) {
        this.messageDao = messageDao;
    }

    @Transactional
    public void createMessage(Message message) {
        messageDao.createMessage(convertToEntity(message));
    }

    @Transactional
    public Message findMessage(long id) {
        return convertToRecord(messageDao.findMessage(id));
    }

    @Transactional
    public List<Message> findAllMessages() {
        return messageDao.findAllMessages().stream().map(MessageService::convertToRecord).toList();
    }

    private static MessageEntity convertToEntity(Message msg) {
        // See https://mkyong.com/java8/java-convert-instant-to-localdatetime/
        return new MessageEntity(
                msg.id(),
                LocalDateTime.ofInstant(msg.timestamp(), ZoneOffset.UTC),
                msg.messageText()
        );
    }

    private static Message convertToRecord(MessageEntity msg) {
        // See https://mkyong.com/java8/java-convert-instant-to-localdatetime/
        return new Message(msg.getId(), msg.getTimestamp().toInstant(ZoneOffset.UTC), msg.getMessageText());
    }
}
