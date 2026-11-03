package com.tpanh.hotel.booking.service.impl;

import com.tpanh.hotel.addon.domain.AddOnService;
import com.tpanh.hotel.addon.repository.AddOnServiceRepository;
import com.tpanh.hotel.booking.domain.*;
import com.tpanh.hotel.booking.domain.addon.BookingAddOn;
import com.tpanh.hotel.booking.dto.request.AddOnSelectionRequest;
import com.tpanh.hotel.booking.dto.request.CancelBookingRequest;
import com.tpanh.hotel.booking.dto.request.CreatePendingBookingRequest;
import com.tpanh.hotel.booking.dto.response.BookingHistoryItemResponse;
import com.tpanh.hotel.booking.dto.response.BookingHistoryRoomTypeResponse;
import com.tpanh.hotel.booking.dto.response.CancelBookingResponse;
import com.tpanh.hotel.booking.dto.response.CancellationEligibilityResponse;
import com.tpanh.hotel.booking.dto.response.CreatePendingBookingResponse;
import com.tpanh.hotel.booking.exception.BookingCancellationNotAllowedException;
import com.tpanh.hotel.booking.exception.BookingConflictException;
import com.tpanh.hotel.booking.exception.BookingNotFoundException;
import com.tpanh.hotel.booking.exception.BookingValidationException;
import com.tpanh.hotel.booking.exception.CapacityExceededException;
import com.tpanh.hotel.booking.exception.InvalidBookingDateException;
import com.tpanh.hotel.booking.mapper.BookingMapper;
import com.tpanh.hotel.booking.policy.BookingCancellationPolicy;
import com.tpanh.hotel.booking.policy.CancellationDecision;
import com.tpanh.hotel.booking.repository.BookingRepository;
import com.tpanh.hotel.booking.service.BookingAccessTokenService;
import com.tpanh.hotel.booking.service.BookingService;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.common.validation.PaginationValidator;
import com.tpanh.hotel.payment.domain.Payment;
import com.tpanh.hotel.payment.domain.PaymentRefund;
import com.tpanh.hotel.payment.domain.PaymentRefundStatus;
import com.tpanh.hotel.payment.domain.PaymentStatus;
import com.tpanh.hotel.payment.domain.PaymentTransaction;
import com.tpanh.hotel.payment.domain.PaymentTransactionStatus;
import com.tpanh.hotel.payment.domain.PaymentTransactionType;
import com.tpanh.hotel.payment.repository.PaymentRepository;
import com.tpanh.hotel.pricing.dto.result.PricingResult;
import com.tpanh.hotel.pricing.service.PricingService;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.room.domain.RoomTypeStatus;
import com.tpanh.hotel.room.exception.RoomTypeNotFoundException;
import com.tpanh.hotel.room.repository.RoomTypeRepository;
import com.tpanh.hotel.room.service.AvailabilityService;
import com.tpanh.hotel.room.service.RoomAvailabilityCalculator;
import com.tpanh.hotel.user.domain.User;
import com.tpanh.hotel.user.exception.UserNotFoundException;
import com.tpanh.hotel.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final int MAX_BOOKING_CODE_RETRY = 5;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String CURRENCY = "VND";

    private final BookingFactory bookingFactory;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final UserRepository userRepository;
    private final AddOnServiceRepository addOnServiceRepository;
    private final BookingCodeGenerator bookingCodeGenerator;
    private final PricingService pricingService;
    private final AvailabilityService availabilityService;
    private final RoomAvailabilityCalculator roomAvailabilityCalculator;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final BookingMapper bookingMapper;
    private final BookingCancellationPolicy cancellationPolicy;
    private final BookingAccessTokenService bookingAccessTokenService;

    @Override
    public CreatePendingBookingResponse createPendingBooking(CreatePendingBookingRequest request) {
        for (int attempt = 1; attempt <= MAX_BOOKING_CODE_RETRY; attempt++) {
            try {
                return transactionTemplate.execute(status -> createPendingBookingInTransaction(request));
            } catch (DataIntegrityViolationException ex) {
                if (!isBookingCodeConflict(ex)) {
                    throw ex;
                }
                if (attempt == MAX_BOOKING_CODE_RETRY) {
                    throw new BookingConflictException("Không thể tạo mã đặt phòng duy nhất. Vui lòng thử lại.");
                }
            }
        }

        throw new BookingConflictException("Không thể tạo mã đặt phòng duy nhất. Vui lòng thử lại.");
    }

    @Override
    public CreatePendingBookingResponse updatePendingBooking(
            UUID bookingId, CreatePendingBookingRequest request, String bookingAccessToken) {
        return transactionTemplate.execute(status -> {
            validateStayDates(request.checkInDate(), request.checkOutDate());
            Booking booking = bookingRepository
                    .findByIdForUpdate(bookingId)
                    .orElseThrow(() -> new BookingValidationException("Không tìm thấy đặt phòng."));
            if (booking.getStatus() != BookingStatus.PENDING
                    || booking.getExpiredAt() == null
                    || !booking.getExpiredAt().isAfter(clock.instant())) {
                throw new BookingValidationException("Booking không còn ở trạng thái PENDING hợp lệ để chỉnh sửa.");
            }
            ensureCheckoutAccess(booking, bookingAccessToken);

            RoomType roomType = getValidatedRoomType(request);
            if (roomAvailabilityCalculator.countAvailableRoomsExcludingBooking(
                            roomType.getId(), request.checkInDate(), request.checkOutDate(), bookingId)
                    <= 0) {
                throw new com.tpanh.hotel.booking.exception.RoomNotAvailableException(
                        "Loại phòng đã hết phòng trống trong khoảng thời gian này.");
            }
            booking.updatePendingDetails(
                    roomType, request.checkInDate(), request.checkOutDate(), request.adults(), request.children());
            bookingRepository.flush();
            populateBooking(booking, request);
            cancelPendingPaymentsWithStaleAmount(booking, clock.instant());
            return toPendingResponse(bookingRepository.save(booking), bookingAccessToken);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingHistoryItemResponse> getCurrentUserBookingHistory(
            int page, int size, BookingStatus status) {
        PaginationValidator.validate(page, size, MAX_PAGE_SIZE);
        UUID userId = currentAuthenticatedUserId();
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("Không tìm thấy tài khoản đang đăng nhập");
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Booking> bookings = status == null
                ? bookingRepository.findAllByBookedById(userId, pageable)
                : bookingRepository.findAllByBookedByIdAndStatus(userId, status, pageable);
        Map<UUID, Payment> latestPayments = findLatestPayments(bookings.getContent());
        return PageResponse.from(
                bookings.map(booking -> toHistoryResponse(booking, latestPayments.get(booking.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingHistoryItemResponse getCurrentUserBooking(String bookingCode) {
        UUID userId = currentAuthenticatedUserId();
        Booking booking = bookingRepository
                .findByBookingCodeAndBookedById(bookingCode, userId)
                .orElseThrow(() -> new BookingNotFoundException("Không tìm thấy đặt phòng"));
        Payment payment = paymentRepository.findAllByBookingIdInOrderByCreatedAtDesc(List.of(booking.getId())).stream()
                .findFirst()
                .orElse(null);
        return toHistoryResponse(booking, payment);
    }

    @Override
    @Transactional(readOnly = true)
    public CancellationEligibilityResponse getCancellationEligibility(String bookingCode) {
        UUID userId = currentAuthenticatedUserId();
        Booking booking = bookingRepository
                .findByBookingCodeAndBookedById(bookingCode, userId)
                .orElseThrow(() -> new BookingNotFoundException("Không tìm thấy đặt phòng"));
        Payment payment = paymentRepository.findAllByBookingIdInOrderByCreatedAtDesc(List.of(booking.getId())).stream()
                .findFirst()
                .orElse(null);
        CancellationDecision decision =
                cancellationPolicy.evaluate(booking, payment == null ? null : payment.getStatus(), clock.instant());
        return toEligibilityResponse(decision);
    }

    @Override
    @Transactional
    public CancelBookingResponse cancelBooking(UUID bookingId, CancelBookingRequest request) {
        UUID userId = currentAuthenticatedUserId();
        Booking booking = bookingRepository
                .findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Không tìm thấy đặt phòng"));
        return cancelLockedBooking(booking, request, userId);
    }

    @Override
    @Transactional
    public CancelBookingResponse cancelBooking(String bookingCode, CancelBookingRequest request) {
        UUID userId = currentAuthenticatedUserId();
        Booking booking = bookingRepository
                .findByBookingCodeForUpdate(bookingCode)
                .orElseThrow(() -> new BookingNotFoundException("Không tìm thấy đặt phòng"));
        return cancelLockedBooking(booking, request, userId);
    }

    private CancelBookingResponse cancelLockedBooking(Booking booking, CancelBookingRequest request, UUID userId) {
        ensureBookingOwner(booking, userId);

        Payment payment = paymentRepository
                .findFirstByBookingIdAndStatusOrderByPaidAtDesc(booking.getId(), PaymentStatus.SUCCESS)
                .orElseGet(() -> paymentRepository
                        .findFirstByBookingIdAndStatusOrderByCreatedAtDesc(booking.getId(), PaymentStatus.PENDING)
                        .orElse(null));
        Instant now = clock.instant();
        CancellationDecision decision =
                cancellationPolicy.evaluate(booking, payment == null ? null : payment.getStatus(), now);
        if (!decision.allowed()) {
            throw new BookingCancellationNotAllowedException(decision.denialCode(), decision.message());
        }
        if (decision.refundAmount() > 0 && request.refundInformation() == null) {
            throw new BookingValidationException("REFUND_INFORMATION_REQUIRED: Cần cung cấp thông tin nhận hoàn tiền");
        }

        String reason = request.normalizedReason();
        booking.cancel(booking.getBookedBy(), reason, now);
        if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
            payment.cancelPending(now);
        } else if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS && decision.refundAmount() > 0) {
            payment.addRefund(PaymentRefund.builder()
                    .amount(BigDecimal.valueOf(decision.refundAmount()))
                    .currency(CURRENCY)
                    .status(PaymentRefundStatus.PENDING)
                    .reason(reason)
                    .idempotencyKey("booking-cancellation:" + booking.getId())
                    .refundMethod(request.refundInformation().method())
                    .bankName(request.refundInformation().normalizedBankName())
                    .accountHolderName(request.refundInformation().normalizedAccountHolderName())
                    .accountNumber(request.refundInformation().normalizedAccountNumber())
                    .requestedAt(now)
                    .build());
            payment.addTransaction(PaymentTransaction.builder()
                    .type(PaymentTransactionType.REFUND)
                    .status(PaymentTransactionStatus.PENDING)
                    .amount(decision.refundAmount())
                    .currency(CURRENCY)
                    .receivedAt(now)
                    .build());
        }

        return new CancelBookingResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getStatus(),
                booking.getCancelledAt(),
                decision.refundStatus(),
                decision.refundPercent(),
                decision.refundAmount(),
                CURRENCY);
    }

    private CancellationEligibilityResponse toEligibilityResponse(CancellationDecision decision) {
        String description = decision.allowed() ? describeRefundPolicy(decision.refundPercent()) : decision.message();
        return new CancellationEligibilityResponse(
                decision.allowed(),
                decision.denialCode(),
                decision.message(),
                decision.refundPercent(),
                decision.refundAmount(),
                CURRENCY,
                description);
    }

    private String describeRefundPolicy(int refundPercent) {
        if (refundPercent > 0) {
            return "Đặt phòng đủ điều kiện hủy và được hoàn " + refundPercent + "% số tiền đã thanh toán";
        }
        return "Đặt phòng chưa thanh toán có thể hủy mà không phát sinh hoàn tiền";
    }

    private void ensureBookingOwner(Booking booking, UUID userId) {
        if (booking.getBookedBy() == null || !booking.getBookedBy().getId().equals(userId)) {
            throw new BookingNotFoundException("Không tìm thấy đặt phòng");
        }
    }

    private Map<UUID, Payment> findLatestPayments(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return Map.of();
        }
        List<UUID> bookingIds = bookings.stream().map(Booking::getId).toList();
        Map<UUID, Payment> payments = new LinkedHashMap<>();
        for (Payment payment : paymentRepository.findAllByBookingIdInOrderByCreatedAtDesc(bookingIds)) {
            payments.putIfAbsent(payment.getBooking().getId(), payment);
        }
        return payments;
    }

    private BookingHistoryItemResponse toHistoryResponse(Booking booking, Payment payment) {
        RoomType roomType = booking.getRoomType();
        PaymentRefund latestRefund = payment == null
                ? null
                : payment.getRefunds().stream()
                        .max(Comparator.comparing(PaymentRefund::getRequestedAt))
                        .orElse(null);
        return new BookingHistoryItemResponse(
                booking.getId(),
                booking.getBookingCode(),
                new BookingHistoryRoomTypeResponse(roomType.getId(), roomType.getName(), roomType.getThumbnailUrl()),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getAdultCount(),
                booking.getChildCount(),
                ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate()),
                BigDecimal.valueOf(booking.getTotalAmount()),
                BigDecimal.valueOf(Math.max(booking.getTotalAmount() - booking.getPaidAmount(), 0L)),
                CURRENCY,
                booking.getStatus(),
                payment == null ? null : payment.getId(),
                payment == null ? null : payment.getStatus(),
                latestRefund == null ? null : latestRefund.getStatus(),
                latestRefund == null ? null : latestRefund.getAmount(),
                booking.getCreatedAt());
    }

    private CreatePendingBookingResponse createPendingBookingInTransaction(CreatePendingBookingRequest request) {
        User guest = null;
        UUID authenticatedUserId = currentAuthenticatedUserIdOrNull();
        if (authenticatedUserId != null) {
            guest = userRepository
                    .findById(authenticatedUserId)
                    .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));
        }

        validateStayDates(request.checkInDate(), request.checkOutDate());

        RoomType roomType = getValidatedRoomType(request);

        availabilityService.ensureAvailable(roomType.getId(), request.checkInDate(), request.checkOutDate());

        String bookingCode = generateBookingCode();

        Booking booking = bookingFactory.createPendingBooking(
                guest,
                bookingCode,
                roomType,
                request.checkInDate(),
                request.checkOutDate(),
                request.adults(),
                request.children());

        String bookingAccessToken = bookingAccessTokenService.generate();
        booking.assignAccessTokenHash(bookingAccessTokenService.hash(bookingAccessToken));

        populateBooking(booking, request);

        bookingRepository.save(booking);

        return toPendingResponse(booking, bookingAccessToken);
    }

    private void ensureCheckoutAccess(Booking booking, String bookingAccessToken) {
        UUID currentUserId = currentAuthenticatedUserIdOrNull();
        boolean owner =
                booking.getBookedBy() != null && booking.getBookedBy().getId().equals(currentUserId);
        if (!owner && !bookingAccessTokenService.matches(bookingAccessToken, booking.getAccessTokenHash())) {
            throw new BookingNotFoundException("Không tìm thấy đặt phòng");
        }
    }

    private void cancelPendingPaymentsWithStaleAmount(Booking booking, Instant now) {
        paymentRepository
                .findAllByBookingIdAndStatusOrderByCreatedAtDesc(booking.getId(), PaymentStatus.PENDING)
                .stream()
                .filter(payment -> payment.getAmount() != booking.getTotalAmount())
                .forEach(payment -> payment.cancelPending(now));
    }

    private CreatePendingBookingResponse toPendingResponse(Booking booking, String bookingAccessToken) {
        CreatePendingBookingResponse response = bookingMapper.toPendingResponse(booking);
        return new CreatePendingBookingResponse(
                response.bookingId(),
                response.bookingCode(),
                response.status(),
                response.totalAmount(),
                response.expiredAt(),
                bookingAccessToken);
    }

    private UUID currentAuthenticatedUserId() {
        UUID userId = currentAuthenticatedUserIdOrNull();
        if (userId == null) {
            throw new BookingValidationException("Bạn cần đăng nhập để xem lịch sử đặt phòng.");
        }
        return userId;
    }

    private UUID currentAuthenticatedUserIdOrNull() {
        return currentUserProvider.currentUserId().orElse(null);
    }

    private RoomType getValidatedRoomType(CreatePendingBookingRequest request) {
        RoomType roomType = roomTypeRepository
                .findByIdForUpdate(request.roomTypeId())
                .orElseThrow(() -> new RoomTypeNotFoundException("Không tìm thấy loại phòng này"));
        if (roomType.getStatus() != RoomTypeStatus.ACTIVE) {
            throw new RoomTypeNotFoundException("Loại phòng này hiện không còn hoạt động");
        }
        if (!roomType.canAccommodate(request.adults(), request.children())) {
            throw new CapacityExceededException(
                    "Số người lớn, trẻ em hoặc tổng số khách vượt quá sức chứa của hạng phòng.");
        }
        return roomType;
    }

    private void populateBooking(Booking booking, CreatePendingBookingRequest request) {
        BookingGuest bookingGuest = BookingGuest.builder()
                .booking(booking)
                .fullName(request.guestInfo().fullName())
                .email(request.guestInfo().email())
                .phone(request.guestInfo().phone())
                .identityNumber(request.guestInfo().identityNumber())
                .isPrimary(true)
                .build();
        booking.addGuest(bookingGuest);

        // 2. Thêm các dịch vụ tiện ích vào Booking
        List<AddOnSelectionRequest> selections = request.addOns() == null ? Collections.emptyList() : request.addOns();
        List<UUID> addOnIds =
                selections.stream().map(AddOnSelectionRequest::addOnServiceId).toList();
        if (addOnIds.stream().distinct().count() != addOnIds.size()) {
            throw new BookingValidationException("Mỗi dịch vụ bổ sung chỉ được chọn một lần.");
        }
        List<AddOnService> services = addOnServiceRepository.findAllByIdInAndActiveTrue(addOnIds);
        Set<UUID> foundAddOnIds = services.stream().map(AddOnService::getId).collect(Collectors.toSet());
        List<UUID> missingAddOnIds = addOnIds.stream()
                .distinct()
                .filter(id -> !foundAddOnIds.contains(id))
                .toList();
        if (!missingAddOnIds.isEmpty()) {
            throw new BookingValidationException("Không tìm thấy dịch vụ tiện ích: " + missingAddOnIds);
        }

        for (AddOnService service : services) {
            int quantity = selections.stream()
                    .filter(selection -> selection.addOnServiceId().equals(service.getId()))
                    .findFirst()
                    .orElseThrow()
                    .quantity();
            BookingAddOn bookingAddOn = BookingAddOn.builder()
                    .addOnService(service)
                    .quantity(quantity)
                    .build();
            bookingAddOn.updateSnapshot(quantity, service.getPrice());

            booking.addAddOn(bookingAddOn);
        }

        // 3. Tính tiền cho Booking
        PricingResult pricingResult = pricingService.calculatePrice(booking);
        booking.applyTotalAmount(pricingResult.totalAmount());

        // 4. Thêm Booking Night cho Booking
        for (BookingNight bookingNight : pricingResult.roomPricingResult().nights()) {
            booking.addNight(bookingNight);
        }
    }

    private boolean isBookingCodeConflict(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        return message != null && message.toLowerCase().contains("booking_code");
    }

    private void validateStayDates(LocalDate checkIn, LocalDate checkOut) {
        LocalDate today = LocalDate.now(clock);

        if (checkIn.isBefore(today)) {
            throw new InvalidBookingDateException("Ngày nhận phòng không được ở quá khứ.");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidBookingDateException("Ngày trả phòng phải sau ngày nhận phòng.");
        }
    }

    private String generateBookingCode() {
        String code;

        do {
            code = bookingCodeGenerator.generate();
        } while (bookingRepository.existsByBookingCode(code));

        return code;
    }
}
