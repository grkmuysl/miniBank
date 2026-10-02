package com.gorkemuysal.minibank.debug;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MeController {

    @GetMapping("/api/v1/me")
    public Map<String, Object> me(Authentication auth) {
        return Map.of(
                "name", auth.getName(),
                "authorities", auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList());
    }
}