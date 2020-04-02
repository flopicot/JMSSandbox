package org.example.jms.messagedrivenbean.consumer;

import org.apache.commons.lang3.NotImplementedException;
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

@MessageDriven(activationConfig = { @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = Resources.ASYNC_QUEUE),
	@ActivationConfigProperty(propertyName = "maxSession", propertyValue = "2"),
	@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue") })
public class MessageReceiverAsync implements MessageListener {
	private static final Logger LOGGER = Logger.getLogger(MessageReceiverAsync.class.getName());
	final Lock lock = new ReentrantLock();
	final Condition waitCond = lock.newCondition();
	
	public void onMessage(Message message) {
		try {
			lock.lock();
			EventMessage eventMessage = SerializationUtils.deserialize(message.getBody(byte[].class));
			if (eventMessage.getValue().contains("11")) {
				throw new NotImplementedException("Simulating message processing error : Cannot process message with the 11 value");
			}
			//Simulate time to process the message
			waitCond.await(500, TimeUnit.MILLISECONDS);
			LOGGER.log(Level.INFO,"{0}-Message received (async): {1} - {2}", new Object[]{MessageReceiverAsync.class.getName(),eventMessage.getType(),eventMessage.getValue()});
		} catch (JMSException | InterruptedException e) {
			throw new IllegalStateException(e);
		} finally {
			lock.unlock();
		}
	}
}
