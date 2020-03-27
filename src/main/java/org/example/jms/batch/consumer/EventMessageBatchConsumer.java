package org.example.jms.batch.consumer;

import org.example.jms.batch.bean.EventMessage;

import javax.jms.Connection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventMessageBatchConsumer extends AbstractBatchConsumer<EventMessage> {
	private static final Logger LOGGER = Logger.getLogger(EventMessageBatchConsumer.class.getName());
	
	// To simulate work
	final Lock lock = new ReentrantLock();
	final Condition waitCond = lock.newCondition();
	
	public EventMessageBatchConsumer(Connection connection, String sourceQueueName, int batchSize) {
		super(connection, sourceQueueName, batchSize);
	}
	
	@Override
	void processMessage(EventMessage eventMessage) {
		try {
			lock.lock();
			//Simulate time to process the message
			waitCond.await(500, TimeUnit.MILLISECONDS);
			LOGGER.log(Level.INFO, AbstractBatchConsumer.class.getName() + "-Message received (async): " + eventMessage.getType() + " - " + eventMessage.getValue());
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, "Exception processing message", e);
		} finally {
			lock.unlock();
		}
	}
}
