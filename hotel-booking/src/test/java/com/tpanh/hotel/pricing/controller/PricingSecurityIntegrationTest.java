package com.tpanh.hotel.pricing.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.authentication.service.impl.JwtService;
import com.tpanh.hotel.authentication.service.impl.JwtService.ValidatedJwt;
import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import com.tpanh.hotel.pricing.mapper.PricingMapper;
import com.tpanh.hotel.pricing.service.PricingService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PricingController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class PricingSecurityIntegrationTest {

    private static final String PATH = "/api/v1/pricing/stay-price-calculations";
    private static final String REQUEST =
            """
            {
              "roomTypeId": "10000000-0000-0000-0000-000000000001",
              "checkInDate": "2026-08-20",
              "checkOutDate": "2026-08-23"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PricingService pricingService;

    @MockitoBean
    private PricingMapper pricingMapper;

    @Test
    void anonymousGuestCanCalculateStayPrice() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedAccountCanAccessWithoutHardCodedRoleRestriction() throws Exception {
        when(jwtService.validate("guest-token"))
                .thenReturn(Optional.of(new ValidatedJwt("guest-id", List.of("CUSTOMER"))));

        mockMvc.perform(post(PATH)
                        .header("Authorization", "Bearer guest-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST))
                .andExpect(status().isOk());
    }
}
