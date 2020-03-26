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

@MessageDriven(activationConfig = { @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = Resources.ASYNC_QUEUE),
	@ActivationConfigProperty(propertyName = "maxSessions", propertyValue = "1"),
	@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue") })
public class MessageReceiverAsync implements MessageListener {
	private static final Logger LOGGER = Logger.getLogger(MessageReceiverAsync.class.getName());
	final Lock lock = new ReentrantLock();
	final Condition waitCond = lock.newCondition();
	
	public void onMessage(Message message) {
		try {
			lock.lock();
			EventMessage eventMessage = SerializationUtils.deserialize(message.getBody(byte[].class));
			//Simulate time to process the message
			waitCond.await(500, TimeUnit.MILLISECONDS);
			System.out.println(MessageReceiverAsync.class.getName()+"-Message received (async): " + eventMessage.getType() + " - "+eventMessage.getValue());
		} catch (JMSException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, null,e);
		} finally {
			lock.unlock();
		}
	}
}
