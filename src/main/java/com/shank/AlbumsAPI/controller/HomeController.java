package com.shank.AlbumsAPI.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class HomeController {
    
    @GetMapping("/")
    public String demo() {
        return "Hello World";
    }

}
