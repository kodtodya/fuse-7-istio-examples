package com.kodtodya.practice.boosters.istio.routes;

import com.kodtodya.practice.boosters.istio.beans.Greetings;
import io.opentracing.Span;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.opentracing.ActiveSpanManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A simple Camel REST DSL route that implement the greetings service.
 * 
 */
@Component
public class GreetingsServiceRoute extends RouteBuilder {

    @Value("${nameService.host}")
    String nameServiceHost;

    @Value("${nameService.port}")
    String nameServicePort;

    private static final String RESPONSE_STRING_FORMAT = "greetings => %s\n";

    @Override
    public void configure() throws Exception {

        // exception handling
        onException(HttpOperationFailedException.class)
                .handled(true)
                .process(this::handleHttpFailure)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(503))
                .end();

        onException(Exception.class)
                .handled(true)
                .transform(simpleF(RESPONSE_STRING_FORMAT, exceptionMessage()) )
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(503))
                .end();

        // @formatter:off
        restConfiguration()
            .component("servlet")
            .enableCORS(true)
            .bindingMode(RestBindingMode.json);
        
        rest("/greetings").description("Greetings REST service")
            .consumes("application/json")
            .produces("application/json")

            .get().outType(Greetings.class)
                .responseMessage().code(200).endResponseMessage()
                .route().routeId("greetings-service-rest-route")
                .pipeline()
                .process(this::addTracer)
                .to("direct:greetingsImpl")
                .transform(simpleF(RESPONSE_STRING_FORMAT, "${body}"))
                .endRest();

        from("direct:greetingsImpl").description("Greetings REST service implementation route")
            .streamCaching()
            .log("Invoking name-service....")
            // invoking name service
            .to("http://" + nameServiceHost + ":" + nameServicePort + "/name?bridgeEndpoint=true")
            .log("name-service invocation : successful....")
            .to("bean:greetingsService?method=getGreetings")
            .marshal().json(JsonLibrary.Jackson);
    }

    public void addTracer(Exchange exchange){
        String userAgent = exchange.getIn().getHeader("user-agent", String.class);
        Span span = ActiveSpanManager.getSpan(exchange);
        span.setBaggageItem("user-agent", userAgent);
    }

    private void handleHttpFailure(Exchange exchange) {
        HttpOperationFailedException exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
        exchange.getOut().setBody(String.format(RESPONSE_STRING_FORMAT,
                String.format("%d %s", exception.getStatusCode(), exception.getResponseBody())
        ));
    }
}