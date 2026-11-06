package com.celiloglu.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {

    @GetMapping({ "/login", "/login.html" })
    public String login() {
        return "login";
    }

    @GetMapping({ "/profile", "/profile.html" })
    public String profile() {
        return "profile";
    }
}
