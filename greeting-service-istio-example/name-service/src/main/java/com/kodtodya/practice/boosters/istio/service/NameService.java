package com.kodtodya.practice.boosters.istio.service;

import com.kodtodya.practice.boosters.istio.beans.Name;

/**
 * Service interface for name service.
 * 
 */
public interface NameService {

    /**
     * Generate Name
     *
     * @return a string name
     */
    Name getName();

}