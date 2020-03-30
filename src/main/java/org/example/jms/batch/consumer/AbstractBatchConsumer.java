package org.example.jms.batch.consumer;

import org.apache.commons.lang3.SerializationUtils;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractBatchConsumer<T> implements Runnable {
	
	private static final Logger LOGGER = Logger.getLogger(AbstractBatchConsumer.class.getName());
	
	private final String sourceQueueName;
	private final int batchSize;
	private final Connection connection;
	private final String selector;
	private boolean mustStop;
	
	public AbstractBatchConsumer(final Connection connection, final String sourceQueueName, final int batchSize, final String selector) {
		this.connection = connection;
		this.batchSize = batchSize;
		this.sourceQueueName = sourceQueueName;
		this.selector = selector;
	}
	
	@Override
	public void run() {
		try {
			connection.start();
			while (!mustStop) {
				processBatch();
			}
			LOGGER.log(Level.INFO,"BatchConsumer {0} stopping.",this);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error processing messages. Batch consumer stopped", e);
		}
	}
	
	private void processBatch() {
		LOGGER.log(Level.INFO, "Starting processing queue.");
		
		UserTransaction userTransaction = retrieveUserTransaction();
		try {
			userTransaction.begin();
			try (Session session = connection.createSession(true, Session.CLIENT_ACKNOWLEDGE)) {
				//Process messages
				processMessages(getMessagesToProcess(session));
			}
			
			userTransaction.commit();
		} catch (Exception e) {
			try {
				userTransaction.rollback();
			} catch (SystemException ex) {
				throw new IllegalStateException("Unable to rollback transaction",e);
			}
			throw new IllegalStateException("Error processing queue",e);
		}
		
		LOGGER.log(Level.INFO, "Stop processing queue.");
	}
	
	private List<T> getMessagesToProcess(Session session) {
		Queue source;
		try {
			source = session.createQueue(this.sourceQueueName);
		} catch (JMSException e) {
			throw new IllegalStateException("Unable to retrieve source queue",e);
		}
		
		try (MessageConsumer consumer = this.selector == null ? session.createConsumer(source) : session.createConsumer(source,this.selector)) {
			final List<T> values = new ArrayList<>();
			while (values.size() < batchSize) {
				Message message = values.isEmpty() ? consumer.receive() : consumer.receive(500);
				if (message == null) {
					break;
				}
				values.add(SerializationUtils.deserialize(message.getBody(byte[].class)));
			}
			return values;
		} catch (JMSException e) {
			throw new IllegalStateException("Unable to retrieve transaction",e);
		}
	}
	
	abstract void processMessages(List<T> messages) throws Exception;
	
	private UserTransaction retrieveUserTransaction() {
		try {
			InitialContext ctx = new InitialContext();
			return (UserTransaction)ctx.lookup("java:comp/UserTransaction");
		} catch (NamingException e) {
			throw new IllegalStateException("Unable to retrieve transaction",e);
		}
	}
	
	public void stop() {
		this.mustStop = true;
	}
}