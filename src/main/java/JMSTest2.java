import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.springframework.jms.connection.JmsTransactionManager;

public class JMSTest2 {
    public static void main(String[] args) throws Exception {
        ActiveMQConnectionFactory jmsConnectionFactory =
                new ActiveMQConnectionFactory(
"tcp://localhost:61616");
        JmsTransactionManager jmsTransactionManager = new JmsTransactionManager(jmsConnectionFactory);

        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setMaximumRedeliveries(-1);
        redeliveryPolicy.setInitialRedeliveryDelay(2000);
        redeliveryPolicy.setRedeliveryDelay(30000);
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
                onException(Exception.class).handled(true).to("activemq:queue:testerrorqueue");

                from("activemq:queue:test?transacted=true")
                        .transacted()
                        .process(exchange -> exchange.getIn().getBody())
                        .log("test123")
                        .process(exchange -> {
                            //System.out.println("Before sleep");
                            //Thread.sleep(10000);
                            System.out.println("Body1=" + exchange.getIn().getBody(String.class));
/*
                            if (exchange.getIn().getBody(String.class).equals("test72")) {
                                // throw new Exception("test70 arrived");
                            }*/
                            System.out.println("Body=" + exchange.getIn().getBody(String.class));
                            /*throw new Exception("Hallo Welt");*/})
                        .to("direct:test2");

            }
        });

        context.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                onException(Exception.class).handled(true).to("activemq:queue:testerrorqueue");

                from("direct:test2")
                        .log(LoggingLevel.INFO, "$body")
                        .process(exchange -> {
                            if (exchange.getIn().getBody(String.class).equals("test70")) {
                                throw new Exception("test70 arrived in direct:test2");
                            }
                        })
                        .to("activemq:queue:test2");
            }
        });

        context.start();
        Thread.sleep(50000);
        context.stop();
    }
}
