package com.kodtodya.practice.boosters.istio.routes;

import io.opentracing.Span;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.HttpMethods;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.apache.camel.opentracing.ActiveSpanManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A simple Camel REST DSL route that implement the training service.
 * 
 */
@Component
public class UserApiRoute extends RouteBuilder {

    @Value("${db.interaction.service.host}")
    String dbInteractionServiceHost;

    @Value("${db.interaction.service.port}")
    String dbInteractionServicePort;

    private static final String RESPONSE_STRING_FORMAT = "User API => %s\n";

    @Override
    public void configure() throws Exception {

        // exception handling
        onException(HttpOperationFailedException.class)
                .handled(true)
                .process(this::handleHttpFailure)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(503))
                .log("${body}")
                .end();

        onException(Exception.class)
                .handled(true)
                .log("${body}")
                .transform(simpleF(RESPONSE_STRING_FORMAT, exceptionMessage()) )
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(503))
                .end();

        // @formatter:off
        restConfiguration()
            .component("servlet")
            .enableCORS(true)
            .bindingMode(RestBindingMode.json);
        
        rest("/tech-talks").description("tech-talks REST service")
                .consumes("application/json")
                .produces("application/json")

                .get("/")
                    .produces("application/json")
                    .route()
                    .setHeader(Exchange.HTTP_METHOD, HttpMethods.GET)
                    .setHeader(Exchange.HTTP_PATH, constant("/"))
                    .to("direct:db-interaction-service-call")
                    .endRest()
                .post("/add")
                    .param().name("name").type(RestParamType.query).description("name of the training").endParam()
                    .param().name("duration").type(RestParamType.query).description("duration of the training").endParam()
                    .param().name("prerequisite").type(RestParamType.query).description("prerequisite of the training").endParam()
                    .produces("application/text")
                    .route()
                    .setHeader(Exchange.HTTP_METHOD, HttpMethods.POST)
                    .setHeader(Exchange.HTTP_PATH, constant("/add"))
                    .to("direct:db-interaction-service-call")
                    .endRest()
                .delete("/remove")
                    .param().name("id").type(RestParamType.query).description("training id to delete").endParam()
                    .produces("application/text")
                    .route()
                    .setHeader(Exchange.HTTP_METHOD, HttpMethods.DELETE)
                    .setHeader(Exchange.HTTP_PATH, constant("/remove"))
                    .to("direct:db-interaction-service-call")
                    .endRest()
                .get("/search")
                    .param().name("id").type(RestParamType.query).description("training id to search").endParam()
                    .produces("application/text")
                    .route()
                    .setHeader(Exchange.HTTP_METHOD, HttpMethods.GET)
                    .setHeader(Exchange.HTTP_PATH, constant("/search"))
                    .to("direct:db-interaction-service-call")
                    .endRest()
        ;

        from("direct:db-interaction-service-call")
            .description("db-interaction-service-call implementation route")
            .routeId("db-interaction-service-rest-route")
            .streamCaching()
            .log("Invoking db-interaction-service....")
            .setHeader(Exchange.HTTP_PATH, simple("/trainings${headers." + Exchange.HTTP_PATH + "}"))
            // invoking db-interaction-service
            .to("http://" + dbInteractionServiceHost + ":" + dbInteractionServicePort + "?bridgeEndpoint=true")
            .log("${body}")
            //.log("db-interaction-service invocation : successful....")
            //.marshal().json(JsonLibrary.Jackson)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
            .pipeline()
            .process(this::addTracer)
            .transform(simpleF(RESPONSE_STRING_FORMAT, "${body}"));
    }

    public void addTracer(Exchange exchange){
        String userAgent = exchange.getIn().getHeader("user-agent", String.class);
        Span span = ActiveSpanManager.getSpan(exchange);
        span.setBaggageItem("user-agent", userAgent);
    }

    private void handleHttpFailure(Exchange exchange) {
        HttpOperationFailedException exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
        exception.printStackTrace();
        exchange.getOut().setBody(String.format(RESPONSE_STRING_FORMAT,
                String.format("%d %s", exception.getStatusCode(), exception.getResponseBody())
        ));
    }
}