package org.example.jms.batch;

import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSDestinationDefinitions;

@JMSDestinationDefinitions({
	@JMSDestinationDefinition(name = ResourcesBatch.ASYNC_QUEUE_BATCH, interfaceName = "javax.jms.Queue", destinationName = ResourcesBatch.ASYNC_QUEUE_BATCH_DESTINATION_NAME, description = "My Async Queue with batch") })
public class ResourcesBatch {
	
	public static final String ASYNC_QUEUE_BATCH = "java:app/jms/myAsyncQueueBatch";
	public static final String ASYNC_QUEUE_BATCH_DESTINATION_NAME = "asyncQueueBatch";
	
}
