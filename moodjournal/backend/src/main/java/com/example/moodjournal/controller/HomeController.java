package com.example.moodjournal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    // This method handles the request to "/hello"
    @GetMapping("/hello")
    public String hello() {
        return "Hello World!";
    }
}
