package com.kodtodya.practice.boosters.istio.beans;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.thrift.internal.senders.HttpSender;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class ConfigBeans {

    @Value("${jaeger.reporter.endpoint}")
    private String jagerReporterEndpoint;

    @Bean
    public io.opentracing.Tracer tracer() {
        final JaegerTracer.Builder builder = new JaegerTracer.Builder("greetings-service");
        builder.withSampler(new ConstSampler(true));

        if(!StringUtils.isEmpty(jagerReporterEndpoint)){
            RemoteReporter.Builder rBuilder = new RemoteReporter.Builder();
            rBuilder.withSender(new HttpSender.Builder(jagerReporterEndpoint).build());
            builder.withReporter(rBuilder.build());
        }

        return builder.build();
    }

    @Bean
    public ServletRegistrationBean camelServletRegistrationBean() {
        ServletRegistrationBean registration = new ServletRegistrationBean(new CamelHttpTransportServlet());
        registration.setName("CamelServlet");
        return registration;
    }
}