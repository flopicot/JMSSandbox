package org.example.jms.messagedrivenbean.consumer;

import org.apache.commons.lang3.SerializationUtils;
import org.example.jms.messagedrivenbean.Resources;
import org.example.jms.messagedrivenbean.bean.EventMessage;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A MDB (Message Driven Bean) to consume the the queue
 */
@MessageDriven(activationConfig = { @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = Resources.ASYNC_QUEUE),
	@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
	@ActivationConfigProperty(propertyName = "maxSession", propertyValue = "2") // Limit the number of concurrent instance of message driven
})
public class MessageAsyncReceiver implements MessageListener {
	
	private static final Logger LOGGER = Logger.getLogger(MessageAsyncReceiver.class.getName());
	
	// To simultate message process time
	final Lock lock = new ReentrantLock();
	final Condition waitCond = lock.newCondition();
	
	public void onMessage(Message message) {
		try {
			lock.lock();
			EventMessage eventMessage = SerializationUtils.deserialize(message.getBody(byte[].class));
			// Uncomment this to test error processing message
			//			if (eventMessage.getValue().contains("11")) {
			//				throw new NotImplementedException("Simulating message processing error : Cannot process message with the 11 value");
			//			}
			//Simulate time to process the message
			waitCond.await(500, TimeUnit.MILLISECONDS);
			LOGGER.log(Level.INFO, "{0}-Message received (async): {1} - {2}",
				new Object[] { MessageAsyncReceiver.class.getName(), eventMessage.getType(), eventMessage.getValue() });
		} catch (JMSException | InterruptedException e) {
			throw new IllegalStateException(e);
		} finally {
			lock.unlock();
		}
	}
}
