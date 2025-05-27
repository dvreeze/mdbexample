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

package eu.cdevreeze.mdbexample.dao;

import eu.cdevreeze.mdbexample.entity.MessageEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

/**
 * Message DAO, implemented using JPA.
 *
 * @author Chris de Vreeze
 */
@RequestScoped
public class MessageDao {

    @PersistenceContext(name = "jpa-unit")
    private EntityManager entityManager;

    public void createMessage(MessageEntity message) {
        entityManager.persist(message);
    }

    public MessageEntity findMessage(long id) {
        return entityManager.find(MessageEntity.class, id);
    }

    public List<MessageEntity> findAllMessages() {
        String query = "SELECT m FROM Message m";
        return entityManager.createQuery(query, MessageEntity.class).getResultList();
    }
}
