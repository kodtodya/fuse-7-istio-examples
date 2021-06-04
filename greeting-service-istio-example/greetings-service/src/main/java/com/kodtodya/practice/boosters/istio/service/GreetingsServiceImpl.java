package com.kodtodya.practice.boosters.istio.service;

import com.kodtodya.practice.boosters.istio.beans.Greetings;
import org.springframework.stereotype.Service;
import org.apache.camel.jsonpath.JsonPath;

@Service("greetingsService")
public class GreetingsServiceImpl implements GreetingsService {

    private static final String THE_GREETINGS = "Hello, ";

    @Override
    public Greetings getGreetings(@JsonPath("$.name") String name ) {
        return new Greetings( THE_GREETINGS + name );
    }

}