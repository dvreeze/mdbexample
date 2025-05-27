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

package eu.cdevreeze.mdbexample.ws;

import eu.cdevreeze.mdbexample.model.Message;
import eu.cdevreeze.mdbexample.service.MessageService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * Message page resource.
 *
 * @author Chris de Vreeze
 */
@RequestScoped
@Path("/message")
public class MessageResource {

    @Inject
    private MessageService messageService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonArray findAllMessages() {
        List<Message> messages = messageService.findAllMessages();

        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        return Json.createArrayBuilder(
                messages.stream().map(msg ->
                        objectBuilder
                                .add("id", msg.id())
                                .add("timestamp", msg.timestamp().toString())
                                .add("messageText", msg.messageText())
                                .build()).toList()
        ).build();
    }
}
