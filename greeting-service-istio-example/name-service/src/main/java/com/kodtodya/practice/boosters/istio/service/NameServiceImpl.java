package com.kodtodya.practice.boosters.istio.service;

import com.kodtodya.practice.boosters.istio.beans.Name;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service("nameService")
public class NameServiceImpl implements NameService {

    @Value("${name.set}")
    private String names;

    @Override
    public Name getName() {
        List<String> nameList = new ArrayList<>(Arrays.asList(names.split(",")));
        Random rand = new Random();
        return new Name(nameList.get(rand.nextInt(nameList.size())));
    }

}