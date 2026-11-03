package com.tpanh.hotel.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(security = {@SecurityRequirement(name = "Xác thực Bearer")})
@SecurityScheme(name = "Xác thực Bearer", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer")
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API ĐẶT PHÒNG KHÁCH SẠN")
                        .description("REST API cho hệ thống đặt phòng khách sạn")
                        .version("v1.0.0")
                        .contact(new Contact().name("Group 5").email("anhtp117@gmail.com")));
    }
}
