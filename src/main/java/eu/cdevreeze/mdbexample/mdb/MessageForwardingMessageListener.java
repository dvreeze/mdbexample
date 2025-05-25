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

package eu.cdevreeze.mdbexample.mdb;

import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJBException;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.MessageDrivenContext;
import jakarta.inject.Inject;
import jakarta.jms.*;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Simple message forwarding message-driven bean. Forwarding the message happens in the same
 * transaction as the call to method "onMessage". On rollback, no forwarding takes place, and
 * the read message is put back on the queue (or its back-out queue), in order to be retried
 * (which may or may not be desirable, or even lead to infinite redelivery, depending on configuration
 * and message settings).
 * <p>
 * Note that at least 3 Jakarta EE specs play a role here: the CDI spec, the Jakarta Messaging spec
 * (JMS) and the EJB spec (in particular for message-driven beans).
 *
 * @author Chris de Vreeze
 */
@MessageDriven(
        activationConfig = {
                @ActivationConfigProperty(
                        propertyName = "destinationLookup", propertyValue = "jms/MdbExampleQueue"),
                @ActivationConfigProperty(
                        propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")
        }
)
public class MessageForwardingMessageListener implements MessageListener {

    private static final Logger logger = Logger.getLogger(MessageForwardingMessageListener.class.getName());

    @Inject
    @JMSConnectionFactory("ConnectionFactory")
    private JMSContext jmsContext;

    @Resource
    private MessageDrivenContext messageDrivenContext;

    @Resource(lookup = "jms/MdbExampleCopiedQueue")
    private Queue copyQueue;

    @Override
    public void onMessage(Message message) {
        // See https://jakarta.ee/specifications/messaging/3.1/jakarta-messaging-spec-3.1#example-using-the-simplified-api-and-injection-4

        logger.info("Entering MessageForwardingMessageListener.onMessage");

        Objects.requireNonNull(jmsContext, "JMSContext must be non-null");
        Objects.requireNonNull(messageDrivenContext, "MessageDrivenContext must be non-null");
        Objects.requireNonNull(copyQueue, "Queue 'jms/MdbExampleCopiedQueue' must be non-null");

        try {
            if (message instanceof TextMessage textMessage) {
                String messageText = textMessage.getText();

                logger.info("Message payload: " + messageText);

                // Sending the message in the same JMS Session and transaction (so may be rolled back)
                jmsContext.createProducer().send(copyQueue, messageText);
            } else {
                logger.warning("Unsupported message type: " + message.getClass().getName());
            }
        } catch (JMSException e) {
            logger.warning("JMSException caught: " + e);
            messageDrivenContext.setRollbackOnly();
            throw new EJBException(e);
        }

        logger.info("Leaving MessageForwardingMessageListener.onMessage (without throwing any exceptions)");
    }
}
