package com.tpanh.hotel.shared.security;

import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("security-test")
class SecurityTestController {

    @PostMapping("/api/v1/auth/login")
    String login() {
        return "public";
    }

    @GetMapping("/api/v1/test/protected")
    String protectedEndpoint() {
        return "protected";
    }

    @PostMapping("/api/v1/ai/chat/stream")
    String unifiedAiChat() {
        return "stream";
    }

    @GetMapping("/api/v1/test/manager")
    @PreAuthorize("hasRole('MANAGER')")
    String managerEndpoint() {
        return "manager";
    }
}
