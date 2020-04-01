package org.example.jms.management;

import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class StartupSingletonManagement {
	
	private static final Logger LOGGER = Logger.getLogger(StartupSingletonManagement.class.getName());
	private static final String JMS_ARTEMIS_BROKER_NAME = "default";
	private static final String HOSTNAME = "localhost";
	private static final String PORT = "9990";
	
	@Schedule(second = "*/10", minute = "*", hour = "*")
	private void reportTimer() {
		try {
			ObjectName on = new ObjectName("org.apache.activemq.artemis:broker=\"" + JMS_ARTEMIS_BROKER_NAME + "\"");
			try (JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL("service:jmx:remote+http://" + HOSTNAME + ":" + PORT), null)) {
				MBeanServerConnection mbsc = connector.getMBeanServerConnection();
				ActiveMQServerControl serverControl = MBeanServerInvocationHandler.newProxyInstance(mbsc, on, ActiveMQServerControl.class, false);
				for (String queueName : serverControl.getQueueNames()) {
					ObjectName queueObjectName = new ObjectName(
						"org.apache.activemq.artemis:broker=\"" + JMS_ARTEMIS_BROKER_NAME + "\",component=addresses,address=\"" + queueName + "\"");
					QueueControl activeMQQueue = MBeanServerInvocationHandler.newProxyInstance(mbsc, queueObjectName, QueueControl.class, false);
					LOGGER.log(Level.INFO,"{0} message count: {1}",new Object[]{queueName,activeMQQueue.getMessageCount()});
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Error retrieving JMS status",e);
		}
	}
}
