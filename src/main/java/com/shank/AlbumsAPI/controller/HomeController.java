package com.shank.AlbumsAPI.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@CrossOrigin(origins = "http://localhost:3000/" , maxAge = 3600)
public class HomeController {
    
    @GetMapping("/")
    public String demo() {
        return "Hello World";
    }

}
