package org.example.jms.batch;

import org.example.jms.batch.bean.EventMessage;
import org.example.jms.batch.bean.Type;
import org.example.jms.batch.consumer.BatchConsumer;
import org.example.jms.batch.producer.MessageAsyncSenderBatch;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedThreadFactory;
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
	
	@Inject
	MessageAsyncSenderBatch messageAsyncSenderBatch;
	@Resource
	private ManagedThreadFactory threadFactory;
	@Resource(lookup = "java:comp/DefaultJMSConnectionFactory")
	private ConnectionFactory connectionFactory;

	@PostConstruct
	private void start(){
		try {
			BatchConsumer<EventMessage> consumer = new BatchConsumer<>(connectionFactory.createConnection(), ResourcesBatch.ASYNC_QUEUE_BATCH_DESTINATION_NAME, 10);
			Thread thread = threadFactory.newThread(consumer);
			thread.start();
		} catch (JMSException e) {
			LOGGER.log(Level.SEVERE,"Error spawning consumer thread",e);
		}
		
	}
	
//	@Schedule(minute="*/1", hour="*")
	private void timer(){
		sendMessages();
	}
	
	
	private void sendMessages() {
		Random random = new Random();
		for (int i = 0; i < 20; i++) {
			int randomInt = random.nextInt();
			EventMessage eventMessage = new EventMessage();
			eventMessage.setType((i % 2 == 0) ? Type.ONE : Type.TWO);
			eventMessage.setValue("toto-"+i+" : "+randomInt);
			messageAsyncSenderBatch.sendMessage(eventMessage);
		}
	}
	
}
