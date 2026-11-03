package com.tpanh.hotel.maintenance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.maintenance.domain.MaintenanceRequest;
import com.tpanh.hotel.maintenance.domain.MaintenanceRequestStatus;
import com.tpanh.hotel.maintenance.dto.request.CreateMaintenanceRequestRequest;
import com.tpanh.hotel.maintenance.dto.request.UpdateMaintenanceRequestStatusRequest;
import com.tpanh.hotel.maintenance.dto.response.MaintenanceRequestListItemResponse;
import com.tpanh.hotel.maintenance.dto.response.MaintenanceRequestResponse;
import com.tpanh.hotel.maintenance.exception.InvalidMaintenanceRequestTransitionException;
import com.tpanh.hotel.maintenance.exception.MaintenanceRequestNotFoundException;
import com.tpanh.hotel.maintenance.mapper.MaintenanceRequestMapper;
import com.tpanh.hotel.maintenance.repository.MaintenanceRequestRepository;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomStatus;
import com.tpanh.hotel.room.exception.RoomNotFoundException;
import com.tpanh.hotel.room.exception.RoomStateConflictException;
import com.tpanh.hotel.room.repository.RoomRepository;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.exception.UserNotFoundException;
import com.tpanh.hotel.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class MaintenanceRequestServiceImplTest {

    private static final UUID REQUEST_ID = UUID.fromString("31000000-0000-0000-0000-000000000001");
    private static final UUID ROOM_ID = UUID.fromString("32000000-0000-0000-0000-000000000001");
    private static final UUID MANAGER_ID = UUID.fromString("33000000-0000-0000-0000-000000000001");
    private static final Instant CHANGED_AT = Instant.parse("2026-09-04T03:00:00Z");

    @Mock
    private MaintenanceRequestRepository maintenanceRequestRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private MaintenanceRequestMapper maintenanceRequestMapper;

    @Mock
    private Clock clock;

    @InjectMocks
    private MaintenanceRequestServiceImpl service;

    @Test
    void createLocksRoomNormalizesInputStartsMaintenanceAndPersistsRequest() {
        Room room = room(RoomStatus.AVAILABLE);
        User manager = manager();
        MaintenanceRequestResponse expected = response(MaintenanceRequestStatus.PENDING, RoomStatus.MAINTENANCE);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(currentUserProvider.currentUserId()).thenReturn(Optional.of(MANAGER_ID));
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(maintenanceRequestRepository.saveAndFlush(any(MaintenanceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(maintenanceRequestMapper.toResponse(any(MaintenanceRequest.class))).thenReturn(expected);

        MaintenanceRequestResponse actual =
                service.create(new CreateMaintenanceRequestRequest(ROOM_ID, "  Điều hòa hỏng  ", "   "));

        ArgumentCaptor<MaintenanceRequest> captor = ArgumentCaptor.forClass(MaintenanceRequest.class);
        verify(maintenanceRequestRepository).saveAndFlush(captor.capture());
        MaintenanceRequest saved = captor.getValue();
        assertThat(actual).isSameAs(expected);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
        assertThat(saved.getRoom()).isSameAs(room);
        assertThat(saved.getCreatedBy()).isSameAs(manager);
        assertThat(saved.getTitle()).isEqualTo("Điều hòa hỏng");
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getStatus()).isEqualTo(MaintenanceRequestStatus.PENDING);
        verify(roomRepository).findByIdForUpdate(ROOM_ID);
    }

    @Test
    void createTrimsNonBlankDescription() {
        Room room = room(RoomStatus.AVAILABLE);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(currentUserProvider.currentUserId()).thenReturn(Optional.of(MANAGER_ID));
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager()));
        when(maintenanceRequestRepository.saveAndFlush(any(MaintenanceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new CreateMaintenanceRequestRequest(ROOM_ID, "TV hỏng", "  Không có tín hiệu  "));

        ArgumentCaptor<MaintenanceRequest> captor = ArgumentCaptor.forClass(MaintenanceRequest.class);
        verify(maintenanceRequestRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("Không có tín hiệu");
    }

    @Test
    void createRejectsMissingOrInvalidInputBeforePersistence() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("request");
        assertThatThrownBy(() -> service.create(new CreateMaintenanceRequestRequest(null, "Title", null)))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("roomId");
        assertThatThrownBy(() -> service.create(new CreateMaintenanceRequestRequest(ROOM_ID, "   ", null)))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("title");
        assertThatThrownBy(() -> service.create(new CreateMaintenanceRequestRequest(ROOM_ID, "x".repeat(151), null)))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("title");
        assertThatThrownBy(
                        () -> service.create(new CreateMaintenanceRequestRequest(ROOM_ID, "Title", "x".repeat(2001))))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("description");

        verifyNoInteractions(roomRepository, maintenanceRequestRepository);
    }

    @Test
    void createReportsMissingRoomAndManager() {
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(new CreateMaintenanceRequestRequest(ROOM_ID, "Title", null)))
                .isInstanceOf(RoomNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo("ROOM_NOT_FOUND");

        Room room = room(RoomStatus.AVAILABLE);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(currentUserProvider.currentUserId()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(new CreateMaintenanceRequestRequest(ROOM_ID, "Title", null)))
                .isInstanceOf(UserNotFoundException.class);

        assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        verify(maintenanceRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsRoomThatIsNotAvailable() {
        Room occupied = room(RoomStatus.OCCUPIED);
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(occupied));
        when(currentUserProvider.currentUserId()).thenReturn(Optional.of(MANAGER_ID));
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager()));

        assertThatThrownBy(() -> service.create(new CreateMaintenanceRequestRequest(ROOM_ID, "Title", null)))
                .isInstanceOf(RoomStateConflictException.class)
                .extracting("errorCode")
                .isEqualTo("ROOM_MAINTENANCE_START_CONFLICT");

        assertThat(occupied.getStatus()).isEqualTo(RoomStatus.OCCUPIED);
        verify(maintenanceRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void findAllUsesValidatedPaginationStableSortAndMapsContent() {
        MaintenanceRequest entity = request(MaintenanceRequestStatus.PENDING, RoomStatus.MAINTENANCE, null, null);
        MaintenanceRequestListItemResponse mapped = listItem();
        Sort expectedSort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"));
        when(maintenanceRequestRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(1, 5, expectedSort), 6));
        when(maintenanceRequestMapper.toListItemResponse(entity)).thenReturn(mapped);

        var result = service.findAll(1, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(maintenanceRequestRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(expectedSort);
        assertThat(result.content()).containsExactly(mapped);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(6);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void findAllRejectsInvalidPagination() {
        assertThatThrownBy(() -> service.findAll(-1, 20)).isInstanceOf(RequestValidationException.class);
        assertThatThrownBy(() -> service.findAll(0, 0)).isInstanceOf(RequestValidationException.class);
        assertThatThrownBy(() -> service.findAll(0, 101)).isInstanceOf(RequestValidationException.class);

        verifyNoInteractions(maintenanceRequestRepository);
    }

    @Test
    void findByIdLoadsDetailsAndMapsResponse() {
        MaintenanceRequest entity = request(MaintenanceRequestStatus.PENDING, RoomStatus.MAINTENANCE, null, null);
        MaintenanceRequestResponse expected = response(MaintenanceRequestStatus.PENDING, RoomStatus.MAINTENANCE);
        when(maintenanceRequestRepository.findDetailsById(REQUEST_ID)).thenReturn(Optional.of(entity));
        when(maintenanceRequestMapper.toResponse(entity)).thenReturn(expected);

        assertThat(service.findById(REQUEST_ID)).isSameAs(expected);
    }

    @Test
    void findByIdRejectsMissingIdAndReportsUnknownRequest() {
        assertThatThrownBy(() -> service.findById(null))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("id");
        when(maintenanceRequestRepository.findDetailsById(REQUEST_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(REQUEST_ID))
                .isInstanceOf(MaintenanceRequestNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo("MAINTENANCE_REQUEST_NOT_FOUND");
    }

    @Test
    void updateStatusTransitionsPendingToInProgressAndKeepsRoomUnderMaintenance() {
        Room room = room(RoomStatus.MAINTENANCE);
        MaintenanceRequest entity = request(MaintenanceRequestStatus.PENDING, room, null, null);
        MaintenanceRequestResponse expected = response(MaintenanceRequestStatus.IN_PROGRESS, RoomStatus.MAINTENANCE);
        prepareUpdate(entity, room, expected);

        MaintenanceRequestResponse actual = service.updateStatus(
                REQUEST_ID, new UpdateMaintenanceRequestStatusRequest(MaintenanceRequestStatus.IN_PROGRESS));

        assertThat(actual).isSameAs(expected);
        assertThat(entity.getStatus()).isEqualTo(MaintenanceRequestStatus.IN_PROGRESS);
        assertThat(entity.getStartedAt()).isEqualTo(CHANGED_AT);
        assertThat(entity.getCompletedAt()).isNull();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
        verify(maintenanceRequestRepository).findByIdForUpdate(REQUEST_ID);
        verify(roomRepository).findByIdForUpdate(ROOM_ID);
    }

    @Test
    void updateStatusCompletesRequestAndReturnsRoomToAvailable() {
        Instant startedAt = CHANGED_AT.minusSeconds(3600);
        Room room = room(RoomStatus.MAINTENANCE);
        MaintenanceRequest entity = request(MaintenanceRequestStatus.IN_PROGRESS, room, startedAt, null);
        MaintenanceRequestResponse expected = response(MaintenanceRequestStatus.COMPLETED, RoomStatus.AVAILABLE);
        prepareUpdate(entity, room, expected);

        service.updateStatus(REQUEST_ID, new UpdateMaintenanceRequestStatusRequest(MaintenanceRequestStatus.COMPLETED));

        assertThat(entity.getStatus()).isEqualTo(MaintenanceRequestStatus.COMPLETED);
        assertThat(entity.getStartedAt()).isEqualTo(startedAt);
        assertThat(entity.getCompletedAt()).isEqualTo(CHANGED_AT);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        verify(maintenanceRequestRepository).saveAndFlush(entity);
    }

    @Test
    void updateStatusRejectsInvalidTransitionWithoutSavingOrChangingRoom() {
        Room room = room(RoomStatus.MAINTENANCE);
        MaintenanceRequest entity = request(MaintenanceRequestStatus.PENDING, room, null, null);
        when(maintenanceRequestRepository.findByIdForUpdate(REQUEST_ID)).thenReturn(Optional.of(entity));
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(clock.instant()).thenReturn(CHANGED_AT);

        assertThatThrownBy(() -> service.updateStatus(
                        REQUEST_ID, new UpdateMaintenanceRequestStatusRequest(MaintenanceRequestStatus.COMPLETED)))
                .isInstanceOf(InvalidMaintenanceRequestTransitionException.class)
                .extracting("errorCode")
                .isEqualTo("MAINTENANCE_REQUEST_STATUS_CONFLICT");

        assertThat(entity.getStatus()).isEqualTo(MaintenanceRequestStatus.PENDING);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
        verify(maintenanceRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateStatusValidatesInputAndReportsMissingRequestOrRoom() {
        assertThatThrownBy(() -> service.updateStatus(REQUEST_ID, null))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("status");
        assertThatThrownBy(() -> service.updateStatus(REQUEST_ID, new UpdateMaintenanceRequestStatusRequest(null)))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("status");
        assertThatThrownBy(() -> service.updateStatus(
                        null, new UpdateMaintenanceRequestStatusRequest(MaintenanceRequestStatus.IN_PROGRESS)))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("id");

        when(maintenanceRequestRepository.findByIdForUpdate(REQUEST_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateStatus(
                        REQUEST_ID, new UpdateMaintenanceRequestStatusRequest(MaintenanceRequestStatus.IN_PROGRESS)))
                .isInstanceOf(MaintenanceRequestNotFoundException.class);

        MaintenanceRequest entity = request(MaintenanceRequestStatus.PENDING, RoomStatus.MAINTENANCE, null, null);
        when(maintenanceRequestRepository.findByIdForUpdate(REQUEST_ID)).thenReturn(Optional.of(entity));
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateStatus(
                        REQUEST_ID, new UpdateMaintenanceRequestStatusRequest(MaintenanceRequestStatus.IN_PROGRESS)))
                .isInstanceOf(RoomNotFoundException.class);

        verify(maintenanceRequestRepository, never()).saveAndFlush(any());
    }

    private void prepareUpdate(MaintenanceRequest entity, Room room, MaintenanceRequestResponse mappedResponse) {
        when(maintenanceRequestRepository.findByIdForUpdate(REQUEST_ID)).thenReturn(Optional.of(entity));
        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(clock.instant()).thenReturn(CHANGED_AT);
        when(maintenanceRequestRepository.saveAndFlush(entity)).thenReturn(entity);
        when(maintenanceRequestMapper.toResponse(entity)).thenReturn(mappedResponse);
    }

    private MaintenanceRequest request(
            MaintenanceRequestStatus status, RoomStatus roomStatus, Instant startedAt, Instant completedAt) {
        return request(status, room(roomStatus), startedAt, completedAt);
    }

    private MaintenanceRequest request(
            MaintenanceRequestStatus status, Room room, Instant startedAt, Instant completedAt) {
        return MaintenanceRequest.builder()
                .id(REQUEST_ID)
                .room(room)
                .title("Điều hòa hỏng")
                .description("Không làm lạnh")
                .status(status)
                .createdBy(manager())
                .startedAt(startedAt)
                .completedAt(completedAt)
                .build();
    }

    private Room room(RoomStatus status) {
        return Room.builder().id(ROOM_ID).roomNumber("0501").status(status).build();
    }

    private User manager() {
        return User.builder().id(MANAGER_ID).fullName("Quản lý HANBAY").build();
    }

    private MaintenanceRequestResponse response(MaintenanceRequestStatus status, RoomStatus roomStatus) {
        return new MaintenanceRequestResponse(
                REQUEST_ID,
                ROOM_ID,
                "0501",
                roomStatus,
                "Điều hòa hỏng",
                "Không làm lạnh",
                status,
                MANAGER_ID,
                "Quản lý HANBAY",
                Instant.parse("2026-09-04T01:00:00Z"),
                Instant.parse("2026-09-04T01:00:00Z"),
                status == MaintenanceRequestStatus.PENDING ? null : CHANGED_AT.minusSeconds(3600),
                status == MaintenanceRequestStatus.COMPLETED ? CHANGED_AT : null);
    }

    private MaintenanceRequestListItemResponse listItem() {
        return new MaintenanceRequestListItemResponse(
                REQUEST_ID,
                ROOM_ID,
                "0501",
                RoomStatus.MAINTENANCE,
                "Điều hòa hỏng",
                MaintenanceRequestStatus.PENDING,
                MANAGER_ID,
                "Quản lý HANBAY",
                Instant.parse("2026-09-04T01:00:00Z"),
                null,
                null);
    }
}
