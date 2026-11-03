package com.tpanh.hotel.addon.dto.response;

import java.util.UUID;

public record AddOnCatalogResponse(UUID id, String code, String name, String description, long price) {}
