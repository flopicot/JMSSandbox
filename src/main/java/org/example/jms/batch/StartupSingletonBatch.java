package org.example.jms.batch;

import org.example.jms.batch.bean.EventMessage;
import org.example.jms.batch.bean.Type;
import org.example.jms.batch.consumer.AbstractBatchConsumer;
import org.example.jms.batch.consumer.EventMessageBatchConsumer;
import org.example.jms.batch.producer.MessageAsyncSenderBatch;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class StartupSingletonBatch {
	
	private static final Logger LOGGER = Logger.getLogger(StartupSingletonBatch.class.getName());
	
	private static final String BATCH_MESSAGE_CONSTANT = "batch_message";
	
	@Inject
	MessageAsyncSenderBatch messageAsyncSenderBatch;
	@Resource
	private ManagedExecutorService managedExecutorService;
	@Resource(lookup = "java:comp/DefaultJMSConnectionFactory")
	private ConnectionFactory connectionFactory;
	
	@PostConstruct
	private void start() {
		try {
			AbstractBatchConsumer<EventMessage> consumerTypeOne = new EventMessageBatchConsumer(connectionFactory.createConnection(),
				ResourcesBatch.ASYNC_QUEUE_BATCH_DESTINATION_NAME, 10, "eventType = 'ONE'");
			managedExecutorService.execute(consumerTypeOne);

			AbstractBatchConsumer<EventMessage> consumerTypeTwo = new EventMessageBatchConsumer(connectionFactory.createConnection(),
				ResourcesBatch.ASYNC_QUEUE_BATCH_DESTINATION_NAME, 10,"eventType = 'TWO'");
			managedExecutorService.execute(consumerTypeTwo);
		} catch (JMSException e) {
			LOGGER.log(Level.SEVERE, "Error spawning consumer thread", e);
		}
	}
	
	@Schedule(minute = "*/1", hour = "*")
	private void timer() {
		sendMessages();
	}
	
	private void sendMessages() {
		Random random = new Random();
		for (int i = 0; i < 20; i++) {
			EventMessage eventMessage = new EventMessage();
			eventMessage.setType((i % 2 == 0) ? Type.ONE : Type.TWO);
			eventMessage.setValue(BATCH_MESSAGE_CONSTANT + "-" + i + " : " + random.nextInt());
			messageAsyncSenderBatch.sendMessage(eventMessage);
		}
	}
	
}
