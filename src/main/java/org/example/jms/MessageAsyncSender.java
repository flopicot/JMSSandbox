package org.example.jms;

import org.example.jms.message.EventMessage;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.CompletionListener;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class MessageAsyncSender {
	private static final Logger LOGGER = Logger.getLogger(MessageAsyncSender.class.getName());
	
	@Inject
	JMSContext jmsContext;

	@Resource(lookup = Resources.ASYNC_QUEUE)
	Queue asyncQueue;
	
	/**
	 * Send a message to the JMS queue.
	 *
	 * @param eventMessage the contents of the message.
	 * @throws JMSRuntimeException if an error occurs in accessing the queue.
	 */
	public void sendMessage(EventMessage eventMessage) throws JMSRuntimeException {
		JMSProducer producer = jmsContext.createProducer();

		try {
			producer.setAsync(new CompletionListener() {
				public void onCompletion(Message msg) {
					try {
						EventMessage eventMessage = msg.getBody(EventMessage.class);
						LOGGER.log(Level.INFO,"Message successfully sent (async): " + eventMessage.getType() + " - "+eventMessage.getValue());
					} catch (JMSException ex) {
						LOGGER.log(Level.SEVERE, null, ex);
					}
				}

				public void onException(Message msg, Exception e) {
					try {
						EventMessage eventMessage = msg.getBody(EventMessage.class);
						LOGGER.log(Level.WARNING,"Message fails to be sent (async): " + eventMessage.getType() + " - "+eventMessage.getValue());
					} catch (JMSException ex) {
						LOGGER.log(Level.SEVERE, null, ex);
					}
				}
			});
		} catch (JMSRuntimeException e) {
			LOGGER.log(Level.SEVERE,"Caught RuntimeException trying to invoke setAsync - not permitted in Java EE. Resorting to synchronous sending...",e);
		}
		producer.send(asyncQueue, eventMessage);
	}
}
