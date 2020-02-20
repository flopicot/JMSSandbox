package org.example.jms;

import org.example.jms.message.EventMessage;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
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
	
	final Lock lock = new ReentrantLock();
	final Condition waitCond = lock.newCondition();
	
	public void onMessage(Message message) {
		try {
			EventMessage eventMessage = message.getBody(EventMessage.class);
			lock.lock();
			waitCond.await(500, TimeUnit.MILLISECONDS);
			System.out.println(MessageReceiverAsync.class.getName()+"-Message received (async): " + eventMessage.getType() + " - "+eventMessage.getValue());
		} catch (JMSException ex) {
			Logger.getLogger(MessageReceiverAsync.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}
}
