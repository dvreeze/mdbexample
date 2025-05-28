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
import eu.cdevreeze.mdbexample.model.MessageData;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Message service.
 *
 * @author Chris de Vreeze
 */
@Stateless
// TransactionManagement annotation value and even annotation itself can be left implicit, since this is the default
@TransactionManagement(TransactionManagementType.CONTAINER)
public class MessageService {

    @Inject
    private final MessageDao messageDao;

    @Inject
    public MessageService(MessageDao messageDao) {
        this.messageDao = messageDao;
    }

    public Message createMessage(MessageData messageData) {
        return convertToRecord(messageDao.createMessage(convertToEntity(messageData)));
    }

    public Message findMessage(long id) {
        return convertToRecord(messageDao.findMessage(id));
    }

    public List<Message> findAllMessages() {
        return messageDao.findAllMessages().stream().map(MessageService::convertToRecord).toList();
    }

    private static MessageEntity convertToEntity(MessageData msg) {
        // See https://mkyong.com/java8/java-convert-instant-to-localdatetime/
        return new MessageEntity(
                null,
                LocalDateTime.ofInstant(msg.timestamp(), ZoneOffset.UTC),
                msg.messageText()
        );
    }

    private static Message convertToRecord(MessageEntity msg) {
        // See https://mkyong.com/java8/java-convert-instant-to-localdatetime/
        return new Message(msg.getId(), msg.getTimestamp().toInstant(ZoneOffset.UTC), msg.getMessageText());
    }
}
