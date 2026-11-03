package com.tpanh.hotel.features.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.domain.RoomTypeStatus;
import com.tpanh.hotel.room.dto.RoomTypeCatalogItemResponse;
import com.tpanh.hotel.room.mapper.RoomTypeMapper;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import com.tpanh.hotel.room.service.impl.RoomTypeServiceImpl;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class RoomTypeCatalogServiceTest {

    @Mock
    private RoomTypeRepository roomTypeRepository;

    private RoomTypeServiceImpl roomTypeCatalogService;

    @BeforeEach
    void setUp() {
        roomTypeCatalogService = new RoomTypeServiceImpl(roomTypeRepository, Mappers.getMapper(RoomTypeMapper.class));
    }

    @Test
    void getCatalogReturnsMappedActiveRoomTypes() {
        RoomType roomType = roomType("Deluxe Room");
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
        when(roomTypeRepository.findByStatusAndNameContainingIgnoreCase(RoomTypeStatus.ACTIVE, "deluxe", pageRequest))
                .thenReturn(new PageImpl<>(List.of(roomType), pageRequest, 1));

        PageResponse<RoomTypeCatalogItemResponse> response =
                roomTypeCatalogService.getCatalog(0, 20, "  deluxe  ", "name", "ASC");

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(roomType.getId());
        assertThat(response.content().get(0).name()).isEqualTo("Deluxe Room");
        assertThat(response.content().get(0).capacityAdults()).isEqualTo(2);
        assertThat(response.content().get(0).capacityChildren()).isEqualTo(1);
        assertThat(response.content().get(0).basePrice()).isEqualByComparingTo("1500000");
        assertThat(response.content().get(0).description()).isEqualTo("Spacious room with one king-size bed");
        assertThat(response.content().get(0).thumbnailUrl()).isEqualTo("https://example.com/images/deluxe-room.jpg");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }

    @Test
    void getCatalogUsesDefaultsForBlankOptionalParameters() {
        PageRequest expectedPageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
        when(roomTypeRepository.findByStatusAndNameContainingIgnoreCase(RoomTypeStatus.ACTIVE, "", expectedPageRequest))
                .thenReturn(new PageImpl<>(List.of(), expectedPageRequest, 0));

        PageResponse<RoomTypeCatalogItemResponse> response = roomTypeCatalogService.getCatalog(0, 20, null, null, null);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    @Test
    void getCatalogCreatesDescendingBasePriceSort() {
        Pageable pageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "basePrice"));
        when(roomTypeRepository.findByStatusAndNameContainingIgnoreCase(RoomTypeStatus.ACTIVE, "", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        roomTypeCatalogService.getCatalog(1, 10, "", "basePrice", "desc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(roomTypeRepository)
                .findByStatusAndNameContainingIgnoreCase(eq(RoomTypeStatus.ACTIVE), eq(""), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getSort().getOrderFor("basePrice").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getCatalogAcceptsMaximumPageSizeAndBlankOptionalParameters() {
        PageRequest pageRequest = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "name"));
        when(roomTypeRepository.findByStatusAndNameContainingIgnoreCase(RoomTypeStatus.ACTIVE, "", pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        PageResponse<RoomTypeCatalogItemResponse> response =
                roomTypeCatalogService.getCatalog(0, 100, "   ", "   ", "   ");

        assertThat(response.size()).isEqualTo(100);
        verify(roomTypeRepository).findByStatusAndNameContainingIgnoreCase(RoomTypeStatus.ACTIVE, "", pageRequest);
    }

    @Test
    void getCatalogRejectsInvalidPage() {
        assertThatThrownBy(() -> roomTypeCatalogService.getCatalog(-1, 20, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Chỉ số trang phải lớn hơn hoặc bằng 0");
        verifyNoInteractions(roomTypeRepository);
    }

    @Test
    void getCatalogRejectsPageSizeAboveMaximum() {
        assertThatThrownBy(() -> roomTypeCatalogService.getCatalog(0, 101, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Số phần tử mỗi trang phải từ 1 đến 100");
        verifyNoInteractions(roomTypeRepository);
    }

    @Test
    void getCatalogRejectsPageSizeBelowMinimum() {
        assertThatThrownBy(() -> roomTypeCatalogService.getCatalog(0, 0, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Số phần tử mỗi trang phải từ 1 đến 100");
        verifyNoInteractions(roomTypeRepository);
    }

    @Test
    void getCatalogRejectsUnsupportedSortField() {
        assertThatThrownBy(() -> roomTypeCatalogService.getCatalog(0, 20, null, "active", "asc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Trường sắp xếp phải là name hoặc basePrice");
        verifyNoInteractions(roomTypeRepository);
    }

    @Test
    void getCatalogRejectsUnsupportedDirection() {
        assertThatThrownBy(() -> roomTypeCatalogService.getCatalog(0, 20, null, "name", "sideways"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Chiều sắp xếp phải là asc hoặc desc");
        verifyNoInteractions(roomTypeRepository);
    }

    private RoomType roomType(String name) {
        return RoomType.builder()
                .id(UUID.randomUUID())
                .code("DELUXE")
                .name(name)
                .description("Spacious room with one king-size bed")
                .capacityAdults(2)
                .capacityChildren(1)
                .maxGuests(3)
                .basePrice(1500000L)
                .thumbnailUrl("https://example.com/images/deluxe-room.jpg")
                .status(RoomTypeStatus.ACTIVE)
                .build();
    }
}
