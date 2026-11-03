package com.tpanh.hotel.frontdesk.service.impl;

import com.tpanh.hotel.booking.domain.Booking;
import com.tpanh.hotel.booking.domain.BookingGuest;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.repository.BookingGuestRepository;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.validation.PaginationValidator;
import com.tpanh.hotel.frontdesk.dto.response.ArrivalRoomTypeResponse;
import com.tpanh.hotel.frontdesk.dto.response.PrimaryGuestSummaryResponse;
import com.tpanh.hotel.frontdesk.dto.response.TodayArrivalResponse;
import com.tpanh.hotel.frontdesk.service.FrontDeskService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FrontDeskServiceImpl implements FrontDeskService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Sort ARRIVAL_SORT = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("bookingCode"));

    private final BookingRepository bookingRepository;
    private final BookingGuestRepository bookingGuestRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TodayArrivalResponse> getTodayArrivals(int page, int size) {
        PaginationValidator.validate(page, size, MAX_PAGE_SIZE);

        LocalDate today = LocalDate.now(clock.withZone(HOTEL_ZONE));
        PageRequest pageable = PageRequest.of(page, size, ARRIVAL_SORT);
        Page<Booking> bookings =
                bookingRepository.findAllByCheckInDateAndStatus(today, BookingStatus.CONFIRMED, pageable);
        Map<UUID, BookingGuest> primaryGuests = findPrimaryGuests(bookings.getContent());

        return PageResponse.from(bookings.map(booking -> toResponse(booking, primaryGuests.get(booking.getId()))));
    }

    private Map<UUID, BookingGuest> findPrimaryGuests(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return Map.of();
        }

        List<UUID> bookingIds = bookings.stream().map(Booking::getId).toList();
        Map<UUID, BookingGuest> primaryGuests =
                bookingGuestRepository.findPrimaryGuestsByBookingIds(bookingIds).stream()
                        .collect(Collectors.toMap(
                                guest -> guest.getBooking().getId(),
                                Function.identity(),
                                (first, duplicate) -> {
                                    log.warn(
                                            "Booking {} has more than one primary guest; using the first result",
                                            first.getBooking().getId());
                                    return first;
                                },
                                LinkedHashMap::new));

        bookings.stream()
                .filter(booking -> !primaryGuests.containsKey(booking.getId()))
                .forEach(booking ->
                        log.warn("Booking {} ({}) has no primary guest", booking.getId(), booking.getBookingCode()));
        return primaryGuests;
    }

    private TodayArrivalResponse toResponse(Booking booking, BookingGuest primaryGuest) {
        PrimaryGuestSummaryResponse guestResponse = primaryGuest == null
                ? null
                : new PrimaryGuestSummaryResponse(
                        primaryGuest.getFullName(), primaryGuest.getPhone(), primaryGuest.getEmail());

        return new TodayArrivalResponse(
                booking.getId(),
                booking.getBookingCode(),
                guestResponse,
                new ArrivalRoomTypeResponse(
                        booking.getRoomType().getId(),
                        booking.getRoomType().getCode(),
                        booking.getRoomType().getName()),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getAdultCount(),
                booking.getChildCount(),
                booking.getStatus(),
                booking.getCreatedAt());
    }
}
