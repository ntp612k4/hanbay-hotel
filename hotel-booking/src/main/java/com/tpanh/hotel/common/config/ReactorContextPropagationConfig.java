package com.tpanh.hotel.common.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

@Configuration
public class ReactorContextPropagationConfig {

    @PostConstruct
    void enable() {
        Hooks.enableAutomaticContextPropagation();
    }

    @PreDestroy
    void disable() {
        Hooks.disableAutomaticContextPropagation();
    }
}
