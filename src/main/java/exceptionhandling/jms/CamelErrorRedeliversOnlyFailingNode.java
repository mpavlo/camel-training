package exceptionhandling.jms;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.SimpleRegistry;

public class CamelErrorRedeliversOnlyFailingNode {
    public static void main(String[] args) throws Exception {
        SimpleRegistry registry = new SimpleRegistry();

        CamelContext context = new DefaultCamelContext(registry);


        context.addRoutes(new RouteBuilder() {
            public void configure() {
                errorHandler(defaultErrorHandler().maximumRedeliveries(3).redeliveryDelay(5000));

                from("direct:test0")
                        .log("test124")
                        .process(exchange -> exchange.getIn().getBody())
                        .log("test123")
                        .to("direct:test1")
                        /*
                        .process(exchange -> {
                            System.out.println("Body1=" + exchange.getIn().getBody(String.class));

                            if (exchange.getIn().getBody(String.class).equals("test72")) {
                                throw new Exception("test72 arrived");
                            }
                            System.out.println("Body=" + exchange.getIn().getBody(String.class));
                        })*/;

                from("direct:test1")
                        .log("test1_123")
                        .process(exchange -> exchange.getIn().getBody())
                        .log("test1_124")
                        .process(exchange -> {
                            System.out.println("test1_Body1=" + exchange.getIn().getBody(String.class));

                            if (exchange.getIn().getBody(String.class).equals("test72")) {
                                throw new Exception("test72 arrived");
                            }
                            System.out.println("Body=" + exchange.getIn().getBody(String.class));
                        });

            }
        });

        ProducerTemplate producerTemplate = context.createProducerTemplate();
        Exchange exchange = new DefaultExchange(context);

        context.start();

        producerTemplate.sendBody("direct:test0", "test72");


        Thread.sleep(10000);
        context.stop();
    }
}
