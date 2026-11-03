package com.tpanh.hotel.room.mapper;

import com.tpanh.hotel.booking.domain.BookingNight;
import com.tpanh.hotel.booking.domain.pricing.BookingNightPriceRule;
import com.tpanh.hotel.common.mapper.MapperConfig;
import com.tpanh.hotel.pricing.dto.result.RoomPricingResult;
import com.tpanh.hotel.room.domain.Amenity;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.domain.RoomTypeImage;
import com.tpanh.hotel.room.dto.AmenityDto;
import com.tpanh.hotel.room.dto.RoomTypeCatalogItemResponse;
import com.tpanh.hotel.room.dto.RoomTypeDetailResponse;
import com.tpanh.hotel.room.dto.response.AvailableRoomTypeResponse;
import com.tpanh.hotel.room.dto.response.NightlyPriceAdjustmentResponse;
import com.tpanh.hotel.room.dto.response.NightlyPriceResponse;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = MapperConfig.class)
public interface RoomTypeMapper {

    @Mapping(target = "description", source = ".", qualifiedByName = "displayDescription")
    RoomTypeCatalogItemResponse toCatalogResponse(RoomType roomType);

    @Mapping(target = "images", source = "roomTypeImages", qualifiedByName = "sortedImageUrls")
    @Mapping(target = "amenities", source = "amenities", qualifiedByName = "sortedAmenities")
    RoomTypeDetailResponse toDetailResponse(RoomType roomType);

    @Mapping(target = "id", source = "roomType.id")
    @Mapping(target = "code", source = "roomType.code")
    @Mapping(target = "name", source = "roomType.name")
    @Mapping(target = "description", source = "roomType", qualifiedByName = "displayDescription")
    @Mapping(target = "basePrice", source = "roomType.basePrice")
    @Mapping(target = "maxGuests", source = "roomType.maxGuests")
    @Mapping(target = "adultCapacity", source = "roomType.capacityAdults")
    @Mapping(target = "childCapacity", source = "roomType.capacityChildren")
    @Mapping(target = "bedType", source = "roomType.bedType")
    @Mapping(target = "areaM2", source = "roomType.areaM2")
    @Mapping(target = "imageUrl", source = "roomType", qualifiedByName = "primaryImageUrl")
    @Mapping(target = "availableRooms", source = "availableRooms")
    @Mapping(target = "totalNights", expression = "java(pricing.nights().size())")
    @Mapping(target = "roomSubtotal", source = "pricing.roomAmount")
    @Mapping(target = "roomDiscount", source = "pricing.roomDiscount")
    @Mapping(target = "roomTotal", source = "pricing.finalRoomAmount")
    @Mapping(target = "nightlyPrices", source = "pricing.nights")
    AvailableRoomTypeResponse toAvailableResponse(RoomType roomType, long availableRooms, RoomPricingResult pricing);

    @Mapping(target = "price", source = "pricePerNight")
    @Mapping(target = "adjustments", source = "appliedRules")
    NightlyPriceResponse toNightlyPriceResponse(BookingNight bookingNight);

    @Mapping(target = "name", source = "priceRule.name")
    @Mapping(target = "ruleType", source = "priceRule.ruleType")
    @Mapping(target = "adjustmentType", source = "priceRule.adjustmentType")
    @Mapping(target = "adjustmentDirection", source = "priceRule.adjustmentDirection")
    @Mapping(target = "adjustmentValue", source = "priceRule.adjustmentValue")
    @Mapping(target = "amount", source = "adjustmentAmount")
    NightlyPriceAdjustmentResponse toNightlyPriceAdjustmentResponse(BookingNightPriceRule appliedRule);

    AmenityDto toAmenityDto(Amenity amenity);

    @Named("displayDescription")
    default String displayDescription(RoomType roomType) {
        String shortDescription = roomType.getShortDescription();
        return shortDescription == null || shortDescription.isBlank() ? roomType.getDescription() : shortDescription;
    }

    @Named("primaryImageUrl")
    default String primaryImageUrl(RoomType roomType) {
        return roomType.getRoomTypeImages().stream()
                .min(Comparator.comparingInt(RoomTypeImage::getSortOrder))
                .map(RoomTypeImage::getImageUrl)
                .orElse(roomType.getThumbnailUrl());
    }

    @Named("sortedImageUrls")
    default List<String> sortedImageUrls(Collection<RoomTypeImage> images) {
        return images.stream()
                .distinct()
                .sorted(Comparator.comparingInt(RoomTypeImage::getSortOrder))
                .map(RoomTypeImage::getImageUrl)
                .toList();
    }

    @Named("sortedAmenities")
    default List<AmenityDto> sortedAmenities(Collection<Amenity> amenities) {
        return amenities.stream()
                .sorted(Comparator.comparing(Amenity::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toAmenityDto)
                .toList();
    }
}
