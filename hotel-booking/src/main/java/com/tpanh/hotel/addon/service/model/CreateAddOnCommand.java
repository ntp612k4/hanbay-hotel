package com.tpanh.hotel.addon.service.model;

public record CreateAddOnCommand(String code, String name, String description, Long price) {}
