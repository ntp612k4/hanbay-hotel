package com.tpanh.hotel.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ExtendStayRequest(@NotNull LocalDate newCheckOutDate) {}
