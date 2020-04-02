package org.example.jms.management;

import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Permit to schedule a report written to log file.
 * Show how to:
 * 	- List destinations (queue or topic)
 * 	- Count message in a queue (with or without filter/selector)
 * 	- Empty a queue
 * 	- Remove a message from queue
 * 	- List message from a queue using a QueueBrowser (with or without filter/selector)
 */
@Singleton
@Startup
public class StartupSingletonManagement {
	
	private static final Logger LOGGER = Logger.getLogger(StartupSingletonManagement.class.getName());
	private static final String JMS_ARTEMIS_BROKER_NAME = "default";
	private static final String HOSTNAME = "localhost";
	private static final String PORT = "9990";
	private static final String QUEUE_NAME_DEAD_LETTER = "jms.queue.DLQ";
	private static final String QUEUE_NAME_EXPIRY = "jms.queue.ExpiryQueue";
	private static final String MESSAGE_FIELD_MESSAGEID = "messageID";
	
	@Schedule(second = "*/10", minute = "*", hour = "*")
	private void reportTimer() {
		try {
			JMXServiceURL jmxServiceURL = new JMXServiceURL("service:jmx:remote+http://" + HOSTNAME + ":" + PORT);
			try (JMXConnector connector = JMXConnectorFactory.connect(jmxServiceURL, null)) {
				MBeanServerConnection mbsc = connector.getMBeanServerConnection();
				for (QueueControl queueControl : getQueues(mbsc)) {
					LOGGER.log(Level.INFO, "{0} message count: {1}", new Object[] { queueControl.getName(), queueControl.getMessageCount() });
					if (QUEUE_NAME_DEAD_LETTER.equals(queueControl.getName()) || QUEUE_NAME_EXPIRY.equals(queueControl.getName())) {
						cleanQueue(queueControl);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error retrieving JMS status", e);
		}
	}
	
	private List<QueueControl> getQueues(MBeanServerConnection mbsc) throws MalformedObjectNameException {
		List<QueueControl> queues = new ArrayList<>();
		ActiveMQServerControl serverControl = getActiveMQServerControl(mbsc);
		for (String queueName : serverControl.getQueueNames()) {
			QueueControl queueControl = getQueueControl(mbsc, queueName);
			queues.add(queueControl);
		}
		return queues;
	}
	
	private QueueControl getQueueControl(MBeanServerConnection mbsc, String queueName) throws MalformedObjectNameException {
		ObjectName queueObjectName = new ObjectName(
			"org.apache.activemq.artemis:broker=\"" + JMS_ARTEMIS_BROKER_NAME + "\",component=addresses,address=\"" + queueName
				+ "\",subcomponent=queues,routing-type=\"anycast\",queue=\"" + queueName + "\"");
		return MBeanServerInvocationHandler.newProxyInstance(mbsc, queueObjectName, QueueControl.class, false);
	}
	
	private ActiveMQServerControl getActiveMQServerControl(MBeanServerConnection mbsc) throws MalformedObjectNameException {
		ObjectName on = new ObjectName("org.apache.activemq.artemis:broker=\"" + JMS_ARTEMIS_BROKER_NAME + "\"");
		return MBeanServerInvocationHandler.newProxyInstance(mbsc, on, ActiveMQServerControl.class, false);
	}
	
	private void cleanQueue(QueueControl queueControl) throws Exception {
		for (Map<String, Object> stringObjectMap : queueControl.listMessages(null)) {
			long messageId = (Long)stringObjectMap.get(MESSAGE_FIELD_MESSAGEID);
			queueControl.removeMessage(messageId);
			LOGGER.log(Level.INFO, "Removing message {0} from {1} queue", new Object[] { messageId, queueControl.getName() });
		}
	}
}
