package org.example.jms.management;

import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;

import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Permit to schedule a report written to log file.
 * Show how to:
 * - List destinations (queue or topic)
 * - Count message in a queue (with or without filter/selector)
 * - Empty a queue
 * - Remove a message from queue
 * - List message from a queue using a QueueBrowser (with or without filter/selector)
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
	private static final String QUEUE_NAME_BEAN = "jms.queue.asyncQueue";
	private static final String QUEUE_NAME_BATCH = "jms.queue.asyncQueueBatch";
	private static final String MESSAGE_FIELD_MESSAGEID = "messageID";
	
	@Resource(lookup = "java:comp/DefaultJMSConnectionFactory")
	private ConnectionFactory connectionFactory;
	
	@Schedule(second = "*/5", minute = "*", hour = "*")
	private void reportTimer() {
		try {
			JMXServiceURL jmxServiceURL = new JMXServiceURL("service:jmx:remote+http://" + HOSTNAME + ":" + PORT);
			try (JMXConnector connector = JMXConnectorFactory.connect(jmxServiceURL, null)) {
				MBeanServerConnection mbsc = connector.getMBeanServerConnection();
				try (Connection connection = connectionFactory.createConnection()) {
					connection.start();
					try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
						for (QueueControl queueControl : getQueues(mbsc)) {
							if (QUEUE_NAME_BEAN.equals(queueControl.getName()) || QUEUE_NAME_BATCH.equals(queueControl.getName())) {
								countMessageFromQueue(queueControl, "eventType = 'ONE'");
								listMessageFromQueue(queueControl, session, "eventType = 'ONE'");
								countMessageFromQueue(queueControl, "eventType = 'TWO'");
								listMessageFromQueue(queueControl, session, "eventType = 'TWO'");
							}
							LOGGER.log(Level.INFO, "{0} message count: {1}", new Object[] { queueControl.getName(), queueControl.getMessageCount() });
							if (QUEUE_NAME_DEAD_LETTER.equals(queueControl.getName()) || QUEUE_NAME_EXPIRY.equals(queueControl.getName())) {
								cleanQueue(queueControl);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error retrieving JMS status", e);
		}
	}
	
	private void countMessageFromQueue(QueueControl queueControl, String filter) throws Exception {
		long count = queueControl.countMessages(filter);
		LOGGER.log(Level.INFO, "count message from queue {0} with filter {1} : {2}", new Object[] { queueControl.getName(), filter, count });
	}
	
	private void listMessageFromQueue(QueueControl queueControl, Session session, String filter) throws Exception {
		LOGGER.log(Level.INFO, "List message from queue {0} with filter {1}", new Object[] { queueControl.getName(), filter });
		Queue queue = session.createQueue(getQueueShortName(queueControl.getName()));
		try (QueueBrowser browser = session.createBrowser(queue, filter)) {
			Enumeration enumeration = browser.getEnumeration();
			while (enumeration.hasMoreElements()) {
				Message message = (Message)enumeration.nextElement();
				LOGGER.log(Level.INFO, "browser : message : messageId = {0}, eventType = {1}",
					new Object[] { message.getJMSMessageID(), message.getStringProperty("eventType") });
			}
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
		// It is better to use QueueControl.removeAllMessages()
		for (Map<String, Object> stringObjectMap : queueControl.listMessages(null)) {
			long messageId = (Long)stringObjectMap.get(MESSAGE_FIELD_MESSAGEID);
			queueControl.removeMessage(messageId);
			LOGGER.log(Level.INFO, "Removing message {0} from {1} queue", new Object[] { messageId, queueControl.getName() });
		}
	}
	
	private String getQueueShortName(String queueFullname) {
		return queueFullname.substring(queueFullname.lastIndexOf('.') + 1, queueFullname.length());
	}
}
