package com.kodtodya.practice.boosters.istio.routes;

import com.kodtodya.practice.boosters.istio.beans.Name;
import io.opentracing.Span;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.opentracing.ActiveSpanManager;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

// A simple Camel REST DSL route that implement the name service.
@Component
public class NameServiceRouter extends RouteBuilder {

    private static final String RESPONSE_STRING_FORMAT = "name-service v1 from '%s': %d\n";

    // Counter to help us see the lifecycle
    private int count = 0;

    private static final String HOSTNAME = parseContainerIdFromHostname( System.getenv().getOrDefault("HOSTNAME", "unknown") );

    static String parseContainerIdFromHostname(String hostname) {
        return hostname.replaceAll("name-service-v\\d+-", "");
    }


    @Override
    public void configure() throws Exception {

        // @formatter:off
        restConfiguration()
            .component("servlet")
            .enableCORS(true)
            .bindingMode(RestBindingMode.auto);
        
        rest("/name")
                .description("Name REST service")
                .consumes("application/json")
                .produces("application/json")

                .get()
                .description("Generate a Name").outType(Name.class)
                .responseMessage().code(200).endResponseMessage()

                .route().routeId("name-service-rest-route")
                    .process(this::addTracer)
                    .process(exchange -> {
                        count++;
                        log.info(String.format("name-service request from %s: %d", HOSTNAME, count));
                    })
                .to("bean:nameService?method=getName")
                .marshal().json(JsonLibrary.Jackson);
    }

    public void addTracer(Exchange exchange){
        String userAgent = (String) exchange.getIn().getHeader("user-agent");
        Span span = ActiveSpanManager.getSpan(exchange);
        span.setBaggageItem("user-agent",userAgent);
    }
}