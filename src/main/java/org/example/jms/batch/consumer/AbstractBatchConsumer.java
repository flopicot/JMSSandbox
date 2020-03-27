package org.example.jms.batch.consumer;

import org.apache.commons.lang3.SerializationUtils;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractBatchConsumer<T> implements Runnable {
	
	private static final Logger LOGGER = Logger.getLogger(AbstractBatchConsumer.class.getName());
	
	private final String sourceQueueName;
	private final int batchSize;
	private final Connection connection;
	
	public AbstractBatchConsumer(final Connection connection, final String sourceQueueName, final int batchSize) {
		this.connection = connection;
		this.batchSize = batchSize;
		this.sourceQueueName = sourceQueueName;
	}
	
	@Override
	public void run() {
		try {
			connection.start();
			while (true) {
				processBatch();
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error processing messages in batch mode", e);
		}
	}
	
	private void processBatch() {
		LOGGER.log(Level.INFO, "Starting processing queue.");
		try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
			//Process messages
				processMessages(getMessagesToProcess(session));
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error processing queue.", e);
		}
		LOGGER.log(Level.INFO, "Stop processing queue.");
	}
	
	private List<T> getMessagesToProcess(Session session) throws JMSException {
		final Queue source = session.createQueue(this.sourceQueueName);
		final List<T> values = new ArrayList<>();
		try (MessageConsumer consumer = session.createConsumer(source)) {
			while (values.size() < batchSize) {
				Message message = consumer.receive(100);
				if (message == null) {
					break;
				}
				values.add(SerializationUtils.deserialize(message.getBody(byte[].class)));
			}
		}
		return values;
	}
	
	abstract void processMessages(List<T> messages);
	
}