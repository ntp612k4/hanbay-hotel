package com.tpanh.hotel.housekeeping.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.housekeeping.dto.response.DirtyRoomResponse;
import com.tpanh.hotel.housekeeping.mapper.DirtyRoomMapper;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.exception.RoomNotFoundException;
import com.tpanh.hotel.room.exception.RoomStateConflictException;
import com.tpanh.hotel.room.repository.RoomRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class HousekeepingServiceImplTest {

    private static final UUID ROOM_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ROOM_TYPE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final Sort DIRTY_ROOM_SORT = Sort.by(Sort.Order.asc("floor"), Sort.Order.asc("roomNumber"));

    @Mock
    private RoomRepository roomRepository;

    private HousekeepingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HousekeepingServiceImpl(roomRepository, Mappers.getMapper(DirtyRoomMapper.class));
    }

    @Test
    void getsDirtyRoomsUsingStableSortAndMapsContract() {
        Room room = dirtyRoom("Kiểm tra minibar");
        PageRequest pageable = PageRequest.of(0, 20, DIRTY_ROOM_SORT);
        when(roomRepository.findAllByStatus(RoomStatus.DIRTY, pageable))
                .thenReturn(new PageImpl<>(List.of(room), pageable, 1));

        var result = service.getDirtyRooms(0, 20);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).singleElement().satisfies(item -> assertDirtyRoom(item, "Kiểm tra minibar"));
        verify(roomRepository).findAllByStatus(RoomStatus.DIRTY, pageable);
    }

    @Test
    void preservesEmptyPageMetadata() {
        PageRequest pageable = PageRequest.of(2, 10, DIRTY_ROOM_SORT);
        when(roomRepository.findAllByStatus(RoomStatus.DIRTY, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = service.getDirtyRooms(2, 10);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    void keepsNullableRoomNote() {
        Room room = dirtyRoom(null);
        PageRequest pageable = PageRequest.of(0, 20, DIRTY_ROOM_SORT);
        when(roomRepository.findAllByStatus(RoomStatus.DIRTY, pageable))
                .thenReturn(new PageImpl<>(List.of(room), pageable, 1));

        var result = service.getDirtyRooms(0, 20);

        assertThat(result.content())
                .singleElement()
                .extracting(DirtyRoomResponse::note)
                .isNull();
    }

    @Test
    void rejectsPageBelowZeroBeforeQueryingDatabase() {
        assertThatThrownBy(() -> service.getDirtyRooms(-1, 20))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("Chỉ số trang phải lớn hơn hoặc bằng 0");

        verifyNoInteractions(roomRepository);
    }

    @Test
    void rejectsSizeOutsideSupportedRangeBeforeQueryingDatabase() {
        assertThatThrownBy(() -> service.getDirtyRooms(0, 0))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("Số phần tử mỗi trang phải từ 1 đến 100");
        assertThatThrownBy(() -> service.getDirtyRooms(0, 101))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("Số phần tử mỗi trang phải từ 1 đến 100");

        verifyNoInteractions(roomRepository);
    }

    @Test
    void startsCleaningFromDirtyRoom() {
        Room room = dirtyRoom(null);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(java.util.Optional.of(room));

        var result = service.startCleaning(ROOM_ID);

        assertThat(result.status()).isEqualTo(RoomStatus.CLEANING);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.CLEANING);
        verify(roomRepository).findByIdForUpdate(ROOM_ID);
    }

    @Test
    void completesCleaningAndMakesRoomAvailable() {
        Room room = roomWithStatus(RoomStatus.CLEANING);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

        var result = service.completeCleaning(ROOM_ID);

        assertThat(result.status()).isEqualTo(RoomStatus.AVAILABLE);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        verify(roomRepository).findByIdForUpdate(ROOM_ID);
    }

    @Test
    void rejectsCompleteCleaningWhenRoomIsNotCleaning() {
        Room available = roomWithStatus(RoomStatus.AVAILABLE);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(available));

        assertThatThrownBy(() -> service.completeCleaning(ROOM_ID))
                .isInstanceOfSatisfying(RoomStateConflictException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("ROOM_CLEANING_COMPLETE_CONFLICT");
                    assertThat(exception.getMessage()).contains("AVAILABLE");
                });
        assertThat(available.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    void rejectsCompleteCleaningWhenRoomDoesNotExist() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeCleaning(ROOM_ID))
                .isInstanceOfSatisfying(RoomNotFoundException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo("ROOM_NOT_FOUND");
                    assertThat(exception.getMessage()).isEqualTo("Không tìm thấy phòng đang dọn");
                });
        verify(roomRepository).findByIdForUpdate(ROOM_ID);
    }

    @Test
    void rejectsInvalidStartCleaningTransition() {
        Room available = roomWithStatus(RoomStatus.AVAILABLE);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(available));

        assertThatThrownBy(() -> service.startCleaning(ROOM_ID))
                .isInstanceOf(RoomStateConflictException.class)
                .hasMessageContaining("Không thể bắt đầu dọn");
    }

    private Room dirtyRoom(String note) {
        return roomWithStatus(RoomStatus.DIRTY, note);
    }

    private Room roomWithStatus(RoomStatus status) {
        return roomWithStatus(status, null);
    }

    private Room roomWithStatus(RoomStatus status, String note) {
        RoomType roomType = RoomType.builder()
                .id(ROOM_TYPE_ID)
                .code("DLX")
                .name("Deluxe hướng biển")
                .basePrice(1_750_000L)
                .maxGuests(3)
                .capacityAdults(2)
                .capacityChildren(1)
                .build();

        return Room.builder()
                .id(ROOM_ID)
                .roomType(roomType)
                .roomNumber("DLX-101")
                .floor(1)
                .status(status)
                .note(note)
                .build();
    }

    private void assertDirtyRoom(DirtyRoomResponse item, String expectedNote) {
        assertThat(item.roomId()).isEqualTo(ROOM_ID);
        assertThat(item.roomNumber()).isEqualTo("DLX-101");
        assertThat(item.floor()).isEqualTo(1);
        assertThat(item.status()).isEqualTo(RoomStatus.DIRTY);
        assertThat(item.note()).isEqualTo(expectedNote);
        assertThat(item.roomType().id()).isEqualTo(ROOM_TYPE_ID);
        assertThat(item.roomType().code()).isEqualTo("DLX");
        assertThat(item.roomType().name()).isEqualTo("Deluxe hướng biển");
    }
}
