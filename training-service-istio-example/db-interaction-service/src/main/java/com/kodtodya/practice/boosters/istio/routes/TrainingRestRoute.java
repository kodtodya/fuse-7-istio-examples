package com.kodtodya.practice.boosters.istio.routes;

import com.kodtodya.practice.boosters.istio.beans.Training;
import com.kodtodya.practice.boosters.istio.service.TrainingService;

import io.opentracing.Span;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.apache.camel.opentracing.ActiveSpanManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TrainingRestRoute extends RouteBuilder {

	private static final String RESPONSE_STRING_FORMAT = "db-interaction-service v1 from '%s': %d\n";

	private int count = 0; // Counter to help us see the lifecycle

	private static final String HOSTNAME = parseContainerIdFromHostname( System.getenv().getOrDefault("HOSTNAME", "unknown") );

	static String parseContainerIdFromHostname(String hostname) {
		return hostname.replaceAll("db-interaction-service-v\\d+-", "");
	}

	@Autowired
	private TrainingService trainingService;

	@Override
	public void configure() throws Exception {

		onException(CamelExecutionException.class)
				.handled(true)
				.redeliveryDelay(2000)
				.maximumRedeliveries(5)
				// asynchronous redelivery
				.asyncDelayedRedelivery()
				// log the exception details
				.to("log:exceptionLogger");

		restConfiguration()
				.component("servlet")
				.enableCORS(true)
				.bindingMode(RestBindingMode.auto);
		//-----------------------------------------------------------------------

		// actual rest service implementation
		rest("/trainings")
				.get("/")
					.produces("application/json")
					.to("direct:fetch-trainings")
				.post("/add")
					.param().name("name")
						.type(RestParamType.query)
						.description("name of the training")
						.endParam()
					.param().name("duration")
						.type(RestParamType.query)
						.description("duration of the training")
						.endParam()
					.param().name("prerequisite")
						.type(RestParamType.query)
						.description("prerequisite of the training")
						.endParam()
					.produces("application/text")
					.to("direct:add-training")
				.delete("/remove")
					.param().name("id").type(RestParamType.query).description("training id to delete").endParam()
					.produces("application/text")
					.to("direct:remove-training")
				.get("/search")
					.param().name("id").type(RestParamType.query).description("training id to search").endParam()
					.produces("application/text")
					.to("direct:search-training")
		;

		// get trainings call
		from("direct:fetch-trainings")
				.log("/trainings request got invoked..")
				.bean(trainingService, "retrieveTrainings")
				.to("seda:tracer-route")
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));

		// add call
		from("direct:add-training")
				.log("/trainings/add request got invoked..")
				.bean(this, "createTraining('${headers.name}', '${headers.duration}', '${headers.prerequisite}')")
				.bean(trainingService, "storeTraining")
				.setBody().constant("training added to training-store")
				.to("seda:tracer-route")
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));

		// remove call
		from("direct:remove-training")
				.log("/trainings/remove request got invoked..")
				.bean(trainingService, "deleteTraining(${headers.id})")
				.setBody().constant("Training removed from list")
				.to("seda:tracer-route")
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));

		// search call
		from("direct:search-training")
				.log("/trainings/search request got invoked..")
				.bean(trainingService, "search(${headers.id})")
				.to("seda:tracer-route")
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200));

		// adding tracer
		from("seda:tracer-route").routeId("db-interaction-service-rest-route")
				.process(this::addTracer)
				.process(exchange -> {
					count++;
					log.info(String.format("db-interaction-service request from %s: %d", HOSTNAME, count));
				});
	}

	public Training createTraining(String name, int duration, String prerequisite) {
		return new Training(name, duration, prerequisite);
	}

	public void addTracer(Exchange exchange){
		String userAgent = (String) exchange.getIn().getHeader("user-agent");
		Span span = ActiveSpanManager.getSpan(exchange);
		span.setBaggageItem("user-agent",userAgent);
	}
}