package org.example.jms.batch.consumer;

import org.example.jms.messagedrivenbean.bean.EventMessage;
import org.example.jms.messagedrivenbean.consumer.MessageReceiverAsync;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BatchConsumer<T>  implements Runnable {
	private static final Logger LOGGER = Logger.getLogger(BatchConsumer.class.getName());
	
	final Session session;
	private final MessageConsumer consumer;
	private final int batchSize;
	
	//To simulate work
	final Lock lock = new ReentrantLock();
	final Condition waitCond = lock.newCondition();
	
	public BatchConsumer(final Connection connection, final String sourceQueue, final int batchSize)
		throws JMSException {
		this.batchSize = batchSize;
		session = connection.createSession(true, Session.CLIENT_ACKNOWLEDGE);
		final Queue source = session.createQueue(sourceQueue);
		consumer = session.createConsumer(source);
	}
	
	@Override
	public void run() {
		while (true) {
			processBatch();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				LOGGER.log(Level.SEVERE,"Error sleeping thread",e);
			}
		}
	}
	
	public void processBatch() {
		LOGGER.log(Level.INFO,"Starting processing queue.");
		final List<T> values = new ArrayList<>();
		try {
			while (values.size() < batchSize) {
				final Message message = consumer.receive();
				values.add(readObject(message));
				message.acknowledge();
			}
			//Process messages
			for (T beanMessage : values) {
				processBeanMessage(beanMessage);
			}
			
			session.commit();
		} catch (Exception e) {
			// Log the exception
			try {
				session.rollback();
			} catch (JMSException re) {
				// Rollback failed, so something fatally wrong.
				throw new IllegalStateException(re);
			}
		}
		LOGGER.log(Level.INFO,"Stop processing queue.");
	}
	
	private void processBeanMessage(T beanMessage) {
		try {
			EventMessage eventMessage = (EventMessage)beanMessage;
			lock.lock();
			//Simulate time to process the message
			waitCond.await(500, TimeUnit.MILLISECONDS);
			System.out.println(MessageReceiverAsync.class.getName()+"-Message received (async): " + eventMessage.getType() + " - "+eventMessage.getValue());
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, null,e);
		} finally {
			lock.unlock();
		}
	}
	
	private T readObject(final Message message) throws JMSException {
		return (T)((ObjectMessage)message).getObject();
	}
}