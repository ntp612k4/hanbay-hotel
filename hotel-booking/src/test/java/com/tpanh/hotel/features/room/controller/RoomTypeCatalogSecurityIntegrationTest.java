package com.tpanh.hotel.features.room.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tpanh.hotel.common.config.SecurityConfig;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.security.JwtAccessDeniedHandler;
import com.tpanh.hotel.common.security.JwtAuthenticationEntryPoint;
import com.tpanh.hotel.room.controller.RoomTypeController;
import com.tpanh.hotel.room.dto.RoomTypeCatalogItemResponse;
import com.tpanh.hotel.room.service.AvailableRoomTypeSearchService;
import com.tpanh.hotel.room.service.RoomTypeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RoomTypeController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
class RoomTypeCatalogSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomTypeService roomTypeService;

    @MockitoBean
    private AvailableRoomTypeSearchService availableRoomTypeSearchService;

    @Test
    void anonymousUserCanViewRoomTypeCatalog() throws Exception {
        PageResponse<RoomTypeCatalogItemResponse> emptyCatalog = new PageResponse<>(List.of(), 0, 6, 0, 0, true, true);
        when(roomTypeService.getCatalog(0, 6, null, "name", "asc")).thenReturn(emptyCatalog);

        mockMvc.perform(get("/api/v1/room-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty());
    }
}
