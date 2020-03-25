package org.example.jms;

import org.example.jms.messagedrivenbean.bean.EventMessage;
import org.example.jms.messagedrivenbean.bean.Type;
import org.example.jms.messagedrivenbean.producer.MessageAsyncSender;

import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.util.Random;

@Singleton
@Startup
public class StartupSingleton {
	@Inject
	MessageAsyncSender messageAsyncSender;

	@PostConstruct
	private void start(){
		sendMessages();
	}
	
	@Schedule(minute="*/1", hour="*")
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
			messageAsyncSender.sendMessage(eventMessage);
		}
	}
	
}
