package com.kodtodya.practice.boosters.istio.service;

import com.kodtodya.practice.boosters.istio.beans.Greetings;


// Service interface for name service.
public interface GreetingsService {

    // Generate Greetings
    // @return a string greetings
    Greetings getGreetings(String name);

}