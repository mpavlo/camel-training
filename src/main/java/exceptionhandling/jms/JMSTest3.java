package exceptionhandling.jms;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.spring.spi.TransactionErrorHandler;
import org.springframework.jms.connection.JmsTransactionManager;

public class JMSTest3 {
    public static void main(String[] args) throws Exception {
        ActiveMQConnectionFactory jmsConnectionFactory =
                new ActiveMQConnectionFactory(
"tcp://localhost:61616");
        JmsTransactionManager jmsTransactionManager = new JmsTransactionManager(jmsConnectionFactory);

        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setMaximumRedeliveries(8);
        redeliveryPolicy.setInitialRedeliveryDelay(2000);
        redeliveryPolicy.setBackOffMultiplier(2);
        //redeliveryPolicy.setRedeliveryDelay(30000);
        redeliveryPolicy.setUseExponentialBackOff(true);

        jmsConnectionFactory.setRedeliveryPolicy(redeliveryPolicy);

        JmsConfiguration jmsConfiguration = new JmsConfiguration();
        jmsConfiguration.setConnectionFactory(jmsConnectionFactory);
        jmsConfiguration.setTransactionManager(jmsTransactionManager);
        jmsConfiguration.setTransacted(true);
        jmsConfiguration.setCacheLevelName("CACHE_CONSUMER");

        SimpleRegistry registry = new SimpleRegistry();
        registry.put("transactionManager", jmsTransactionManager);

        CamelContext context = new DefaultCamelContext(registry);
        ActiveMQComponent activeMQComponent = new ActiveMQComponent();
        activeMQComponent.setConfiguration(jmsConfiguration);

        context.addComponent("activemq", activeMQComponent);

        context.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                errorHandler(defaultErrorHandler().maximumRedeliveries(10).redeliveryDelay(10000));

                from("activemq:queue:test?transacted=true&acknowledgementModeName=CLIENT_ACKNOWLEDGE")
                        .transacted()
                        .process(exchange -> exchange.getIn().getBody())
                        .log("test123")
                        .process(exchange -> {
                            System.out.println("Body1=" + exchange.getIn().getBody(String.class));

                            if (exchange.getIn().getBody(String.class).equals("test72")) {
                                throw new Exception("test72 arrived");
                            }
                            System.out.println("Body=" + exchange.getIn().getBody(String.class));
                            /*throw new Exception("Hallo Welt");*/});

            }
        });

        context.start();
        Thread.sleep(50000);
        context.stop();
    }
}
