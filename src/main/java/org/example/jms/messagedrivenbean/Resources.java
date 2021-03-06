package org.example.jms.messagedrivenbean;

import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSDestinationDefinitions;

@JMSDestinationDefinitions({
	@JMSDestinationDefinition(name = Resources.ASYNC_QUEUE, /**resourceAdapter = "jmsra",**/ interfaceName = "javax.jms.Queue", destinationName = Resources.ASYNC_QUEUE_DESTINATION_NAME, description = "My Async Queue") })
public class Resources {
	
	public static final String ASYNC_QUEUE = "java:app/jms/myAsyncQueue";
	public static final String ASYNC_QUEUE_DESTINATION_NAME = "asyncQueue";
	
}
