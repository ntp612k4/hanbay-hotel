package com.tpanh.hotel.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({PaymentProperties.class, SePayProperties.class})
public class PaymentConfiguration {}
