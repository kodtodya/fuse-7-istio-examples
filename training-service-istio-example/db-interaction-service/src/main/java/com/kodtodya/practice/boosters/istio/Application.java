package com.kodtodya.practice.boosters.istio;

import org.apache.camel.opentracing.starter.CamelOpenTracing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@CamelOpenTracing
@SpringBootApplication
public class Application {
     // Main method to start the application.
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}