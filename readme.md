# JMSSandbox - Sandbox project to poc JMS uses cases

Tested on a Wildfly 18.0.1 Final

## List of features contained into code example

### package org.example.jms.messagedrivenbean

* Transmit JMS message containing serialized java object asynchronously
* Declare the queue with the @JMSDestinationDefinition annotation
* A Stateless message sender/producer
* A scheduler using the producer to send message periodically
* A Message Driven Bean (MDB - @MessageDriven) to consume the message in the queue
  * Limit the number of concurrent consumer/MDB
  * Simulate time consuming and processing messages

### package org.example.jms.batch
* Transmit JMS message containing serialized java object asynchronously
* Declare the queue with the @JMSDestinationDefinition annotation
* A Stateless message sender/producer
* A scheduler using the producer to send message periodically
* An AbstractBatchConsumer<T> Runnable class to consume JMS message with T body type
  * a batchSize to configure the number of message to process each time
  * a transaction for each batch/lot to be able to rollback the message processing in cas of error
  * In case of rollback, message are redelivered
  * A selector permit to discriminate the message to process in the queue
  * the Runnable task can be asked for stop.
* A EventMessageBatchConsumer implementation for AbstractBatchConsumer<EventMessage>
  * Process a batch/lot of EventMessage typed messages (size configure with batchSize)
  * Simulate time to process message
* Starting multiple Runnable task to consume message from the queue with discrimination using filter/selector

### package org.example.jms.management
* A scheduler to write a report on queues state periodically into server log
* List the existing queues
* Count message from each queue (with or without discrimination using filter/selector)
* List message from queue (with or without discrimination using filter/selector)
* Empty a queue
* Remove a specific message from a queue

## Minimal Wildfly configuration

### Add the wildfly extension (standalone.xml)

	<extension module="org.wildfly.extension.messaging-activemq"/>

### Define the JMS subsystem configuration (standalone.xml)

	<subsystem xmlns="urn:jboss:domain:messaging-activemq:8.0">
		<server name="default">
			<statistics enabled="${wildfly.messaging-activemq.statistics-enabled:${wildfly.statistics-enabled:false}}"/>
			<security-setting name="#">
				<role name="guest" send="true" consume="true" create-non-durable-queue="true" delete-non-durable-queue="true"/>
			</security-setting>
			<address-setting name="#" max-size-bytes="10485760" page-size-bytes="2097152" message-counter-history-day-limit="10"/>
			<in-vm-connector name="in-vm" server-id="0"/>
			<in-vm-acceptor name="in-vm" server-id="0">
				<param name="buffer-pooling" value="false"/>
			</in-vm-acceptor>
			<pooled-connection-factory name="activemq-ra" transaction="xa" connectors="in-vm" entries="java:/JmsXA java:jboss/DefaultJMSConnectionFactory" confirmation-window-size="1024"/>
		</server>
	</subsystem>
	
### Define the default JMS connection factory into default bindings (standalone.xml)
	<default-bindings ...  jms-connection-factory="java:jboss/DefaultJMSConnectionFactory" ... />

## Other Wildfly configuration tips

### Define queues or topics administratively (standalone.xml)

	<subsystem xmlns="urn:jboss:domain:messaging-activemq:8.0">
		<server name="default">
		[...]
		<jms-queue name="testQueue"
				   entries="jms/queue/test java:jboss/exported/jms/queue/test" />
		<jms-topic name="testTopic"
				   entries="jms/topic/test java:jboss/exported/jms/topic/test" />
	</subsystem>
	
This declaration can be replace by the use of @JMSDestinationDefinition into Java sources

### Define Dead letter and or Expiry queue

* If configured, dead letter queue will receive message which cannot be delivered or redelivered successfully. If not this messages will be dropped and a log is written.
* If configured, expiry queue will receive expirated messages. If not this messages will be dropped and a log is written.


    <subsystem xmlns="urn:jboss:domain:messaging-activemq:8.0">
        <server name="default">
            ...
            <address-setting name="#" dead-letter-address="jms.queue.DLQ" expiry-address="jms.queue.ExpiryQueue" max-size-bytes="10485760" page-size-bytes="2097152" message-counter-history-day-limit="10"/>
            ...
            <jms-queue name="ExpiryQueue" entries="java:/jms/queue/ExpiryQueue"/>
            <jms-queue name="DLQ" entries="java:/jms/queue/DLQ"/>
            ....
        </server>
    </subsystem>

### Configure the consumer prefetch size (confirmation-window-size attribute on connection factory)

	<subsystem xmlns="urn:jboss:domain:messaging-activemq:8.0">
		<server name="default">
		...
		    <pooled-connection-factory name="activemq-ra" transaction="xa" connectors="in-vm" entries="java:/JmsXA java:jboss/DefaultJMSConnectionFactory" confirmation-window-size="1024" consumer-window-size="0"/>
		...
		</server>
    </subsystem>

### Use JDBC persistence / Datasource instead of the default file journal mode
Warning: on huge queue (near 30 000 little byte messages), artemis try to activate pagination and fails to do this on mysql backend. It works on standard file persistence.  

#### Configure the database driver and the datasource (standalone.xml)

    <subsystem xmlns="urn:jboss:domain:datasources:5.0">
        <datasources>
            <drivers>
                <driver module="org.mysql" name="mysql">
                    <xa-datasource-class>com.mysql.jdbc.jdbc2.optional.MysqlXADataSource</xa-datasource-class>
                    <driver-class>com.mysql.jdbc.Driver</driver-class>
                </driver>
            </drivers>
            <datasource jndi-name="java:jboss/datasources/jms-ds" jta="true" pool-name="jms-ds">
                <connection-url>jdbc:mysql://localhost:3306/jms_test?useUnicode=yes&amp;characterEncoding=UTF-8&amp;useSSL=false</connection-url>
                <driver>mysql</driver>
                <security>
                    <user-name>user</user-name>
                    <password>password</password>
                </security>
                <validation>
                    <background-validation>true</background-validation>
                    <background-validation-millis>10000</background-validation-millis>
                    <valid-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLValidConnectionChecker"/>
                    <exception-sorter class-name="org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLExceptionSorter"/>
                </validation>
                <pool>
                    <min-pool-size>5</min-pool-size>
                    <max-pool-size>50</max-pool-size>
                    <prefill>true</prefill>
                </pool>
            </datasource>
        </datasources>
    </subsystem>

#### Configure the JMS journal to use the datasource

    <subsystem xmlns="urn:jboss:domain:messaging-activemq:8.0">
        <server name="default">
            <journal datasource="jms-ds"/>
            ...
        </server>
    </subsystem>
    
to change table names (names limited to 10 characters)

    <subsystem xmlns="urn:jboss:domain:messaging-activemq:8.0">
        <server name="default">
            <journal datasource="jms-ds" bindings-table="jms_bindings" messages-table="jms_msg" page-store-table="jms_pg_str" large-messages-table="jms_lg_msg"/>
            ...
        </server>
    </subsystem>
    
### Define redelivery behaviour (standalone.xml)

### Define the max delivery attempts for messages (default 10)

    <subsystem xmlns="urn:jboss:domain:messaging-activemq:8.0">
        <server name="default">
            ...
            <address-setting name="#" dead-letter-address="jms.queue.DLQ" expiry-address="jms.queue.ExpiryQueue" max-size-bytes="10485760" page-size-bytes="2097152" message-counter-history-day-limit="10" max-delivery-attempts="3"/>
            ...
        </server>
    </subsystem>
    
### Define the redelivery delay for messages

    <subsystem xmlns="urn:jboss:domain:messaging-activemq:8.0">
        <server name="default">
            ...
            <address-setting name="#" dead-letter-address="jms.queue.DLQ" expiry-address="jms.queue.ExpiryQueue" max-size-bytes="10485760" page-size-bytes="2097152" message-counter-history-day-limit="10" redelivery-delay="20000" redelivery-multiplier="1.5"/>
            ...
        </server>
    </subsystem>     
    
### Discriminate message consuming a queue

Add a property to the message when sending

    JMSProducer producer = jmsContext.createProducer();
    BytesMessage bytesMessage = jmsContext.createBytesMessage();
    bytesMessage.writeBytes(SerializationUtils.serialize(eventMessage));
    ...
    bytesMessage.setStringProperty("type",eventMessage.getType().name());
    producer.send(asyncQueueBatch, bytesMessage);

Consume messages from queue using a Consumer with selector

    String selector = "type = 'ONE'";
    MessageConsumer consumer = session.createConsumer(source,this.selector);
    Message message = consumer.receive();
    
The filter (selector) language documentation can be found here: https://docs.oracle.com/javaee/7/api/javax/jms/Message.html

### Activate jmx management

    <subsystem xmlns="urn:jboss:domain:messaging-activemq:8.0">
        <server name="default">
            <management jmx-enabled="true"/>
            ...
        </server>
    </subsystem>

### limit the number of consumer thread (standalone.xml)
To limit the number of consumer thread to 2

	<subsystem xmlns="urn:jboss:domain:ejb3:6.0">
		...
		<mdb>
			<resource-adapter-ref resource-adapter-name="${ejb.resource-adapter-name:activemq-ra.rar}"/>
			<bean-instance-pool-ref pool-name="event-mdb-process"/>
		</mdb>
		<pools>
                <bean-instance-pools>
					<strict-max-pool name="event-mdb-process" max-pool-size="2" instance-acquisition-timeout="5" instance-acquisition-timeout-unit="MINUTES"/>
					...
				</bean-instance-pools>
		</pools>
		...
	</subsystem>
    
## NOTES

### Performance on Message driven bean consumer versus Runnable batching message by 10
A runnable task consuming message batching with lot of 10 appears to be 2x faster than Message Driven Bean and his onMessage() call (with default configuration).  

### Three connector types exists:
 * in-vm-connector can be used by a local client (i.e. one running in the same JVM as the server)
 * remote-connector can be used by a remote client (and uses Netty over TCP for the communication)
 * http-connector can be used by a remote client (and uses Undertow Web Server to upgrade from a HTTP connection)
 
### There are three kinds of basic JMS connection-factory

### Producer and consumer declaration and uses
 * JNDI / Injected (Produces) (@Inject)
 * MDB -> Message driven bean (consumes) (@MessageDriven)

### The default jsm connection factory
Since JMS 2.0 -> default JMS connection factory is accessible to EE application under the JNDI name java:comp/DefaultJMSConnectionFactory

### Deploy ...-jms.xml files to define a JMS configuration

	<!--This feature is primarily intended for development as destinations deployed this way can not be managed with any of the provided management tools (e.g. console, CLI, etc)-->
	<?xml version="1.0" encoding="UTF-8"?>
	<messaging-deployment xmlns="urn:jboss:messaging-activemq-deployment:1.0">
	   <server name="default">
		  <jms-destinations>
			 <jms-queue name="sample">
				<entry name="jms/queue/sample"/>
				<entry name="java:jboss/exported/jms/queue/sample"/>
			 </jms-queue>
		  </jms-destinations>
	   </server>
	</messaging-deployment>

### Artemis programmatic management
See org.apache.activemq.artemis.api.core.management.ActiveMQServerControl if it can help

## Documentation sources

### Jms resources management
    https://activemq.apache.org/components/artemis/documentation/javadocs/javadoc-1.2.0/org/apache/activemq/artemis/api/jms/management/package-summary.html
    https://activemq.apache.org/components/artemis/documentation/1.3.0/management.html

### The Jboss native management API :
	https://docs.jboss.org/author/display/WFLY10/The+native+management+API

### Messaging configuration
	https://docs.jboss.org/author/display/WFLY10/Messaging+configuration
	
### JMS batch consuming:
	https://stackoverflow.com/questions/53960889/batch-bulk-message-jms-processing-with-wildfly

### SIMPLE JMS BATCH COMPONENT
    https://camel.apache.org/components/latest/sjms-batch-component.html
    
### Using Advanced JMS Features (Persistence, Message Priority and TTL, temporary destinations, local transaction)
    https://javaee.github.io/tutorial/jms-concepts004.html#BNCFY
    
### JMS DB Persistence
    https://activemq.apache.org/persistence