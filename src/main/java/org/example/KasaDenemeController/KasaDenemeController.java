package org.example.KasaDenemeController;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KasaDenemeController {
    @GetMapping("/api/test")
    public String test() {
        return "Backend çalışıyor!";
    }
}
