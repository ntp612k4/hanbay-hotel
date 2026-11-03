package com.tpanh.hotel.room.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.common.exception.ResourceNotFoundException;
import com.tpanh.hotel.room.domain.Amenity;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.domain.RoomTypeImage;
import com.tpanh.hotel.room.domain.RoomTypeStatus;
import com.tpanh.hotel.room.mapper.RoomTypeMapper;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import com.tpanh.hotel.room.service.impl.RoomTypeServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomTypeServiceTest {
    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Test
    void getDetailReturnsOnlyActiveRoomTypeWithSortedData() {
        UUID id = UUID.randomUUID();
        RoomType roomType = RoomType.builder()
                .id(id)
                .code("DELUXE")
                .name("Deluxe")
                .shortDescription("Mô tả ngắn")
                .description("Mô tả")
                .basePrice(2_000_000)
                .maxGuests(4)
                .capacityAdults(2)
                .capacityChildren(2)
                .build();
        RoomTypeImage image = mock(RoomTypeImage.class);
        when(image.getImageUrl()).thenReturn("room.jpg");
        roomType.getRoomTypeImages().add(image);
        Amenity amenity = mock(Amenity.class);
        UUID amenityId = UUID.randomUUID();
        when(amenity.getId()).thenReturn(amenityId);
        when(amenity.getName()).thenReturn("Wi-Fi");
        when(amenity.getIcon()).thenReturn("wifi");
        roomType.getAmenities().add(amenity);
        when(roomTypeRepository.findDetailByIdAndStatus(id, RoomTypeStatus.ACTIVE))
                .thenReturn(Optional.of(roomType));

        var response =
                new RoomTypeServiceImpl(roomTypeRepository, Mappers.getMapper(RoomTypeMapper.class)).getDetail(id);

        assertEquals(id, response.id());
        assertEquals(BigDecimal.valueOf(2_000_000), response.basePrice());
        assertEquals(2, response.capacityAdults());
        assertEquals(2, response.capacityChildren());
        assertEquals(List.of("room.jpg"), response.images());
        assertEquals(amenityId, response.amenities().get(0).id());
    }

    @Test
    void getDetailReturnsNotFoundWhenRoomTypeIsNotActive() {
        UUID id = UUID.randomUUID();
        when(roomTypeRepository.findDetailByIdAndStatus(id, RoomTypeStatus.ACTIVE))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> new RoomTypeServiceImpl(
                                roomTypeRepository, Mappers.getMapper(RoomTypeMapper.class))
                        .getDetail(id));

        assertEquals("Không tìm thấy loại phòng đang kinh doanh", exception.getMessage());
        verify(roomTypeRepository).findDetailByIdAndStatus(id, RoomTypeStatus.ACTIVE);
    }
}
