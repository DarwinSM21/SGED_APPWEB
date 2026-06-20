package org.uteq.backend.auth.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/public")
    public String publico() {
        return "Publico";
    }

    @GetMapping("/private")
    public String privado() {
        return "Privado";
    }
}