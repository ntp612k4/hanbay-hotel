package com.tpanh.hotel.booking.domain;

import com.tpanh.hotel.booking.domain.addon.BookingAddOn;
import com.tpanh.hotel.booking.exception.BookingValidationException;
import com.tpanh.hotel.booking.exception.InvalidBookingStateException;
import com.tpanh.hotel.common.domain.BaseEntity;
import com.tpanh.hotel.room.domain.Room;
import com.tpanh.hotel.room.domain.RoomType;
import com.tpanh.hotel.user.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.type.SqlTypes;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(
        name = "bookings",
        indexes = {
            @Index(name = "idx_booked_by", columnList = "booked_by"),
            @Index(name = "idx_room_booking", columnList = "room_type_id, check_in_date, check_out_date"),
            @Index(name = "idx_status", columnList = "status")
        })
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @EqualsAndHashCode.Include
    @Column(name = "booking_code", unique = true, nullable = false, length = 50)
    @NaturalId
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_by")
    private User bookedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_room_id")
    private Room assignedRoom;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Builder.Default
    @Column(name = "adult_count", nullable = false)
    private int adultCount = 1;

    @Builder.Default
    @Column(name = "child_count", nullable = false)
    private int childCount = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Builder.Default
    @Column(name = "total_amount", nullable = false)
    private long totalAmount = 0L;

    @Builder.Default
    @Column(name = "paid_amount", nullable = false)
    private long paidAmount = 0L;

    @Column(name = "access_token_hash", length = 64)
    private String accessTokenHash;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingGuest> guests = new ArrayList<>();

    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingNight> nights = new ArrayList<>();

    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingStatusHistory> statusHistories = new ArrayList<>();

    @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BookingAddOn> addOns = new ArrayList<>();

    public void recordCreated(User createdBy) {

        BookingStatusHistory history = BookingStatusHistory.builder()
                .oldStatus(null)
                .newStatus(BookingStatus.PENDING)
                .changedBy(createdBy)
                .reason("Người dùng đặt phòng")
                .build();

        addStatusHistory(history);
    }

    public void addGuest(BookingGuest guest) {
        Objects.requireNonNull(guest, "Guest không được để trống");

        String identity = guest.getIdentityNumber();
        if (identity != null && !identity.trim().isEmpty()) {
            String normalizedIdentity = identity.trim();

            boolean isDuplicate = this.guests.stream()
                    .map(BookingGuest::getIdentityNumber)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .anyMatch(normalizedIdentity::equals);

            if (isDuplicate) {
                throw new BookingValidationException("Khách hàng với CMND/CCCD này đã tồn tại trong Booking.");
            }
        }

        this.guests.add(guest);
        guest.assignBooking(this);
    }

    public void removeGuest(BookingGuest guest) {
        Objects.requireNonNull(guest, "Guest không được để trống");
        this.guests.remove(guest);
        guest.assignBooking(null);
    }

    public void addNight(BookingNight night) {
        Objects.requireNonNull(night, "Night không được để trống");

        boolean isDuplicate = this.nights.stream().anyMatch(n -> n.getStayDate().equals(night.getStayDate()));

        if (isDuplicate) {
            throw new BookingValidationException("Ngày lưu trú " + night.getStayDate() + " đã tồn tại trong Booking.");
        }
        this.nights.add(night);
        night.assignBooking(this);
    }

    public void removeNight(BookingNight night) {
        Objects.requireNonNull(night, "Night không được để trống");
        this.nights.remove(night);
        night.assignBooking(null);
    }

    private void addStatusHistory(BookingStatusHistory history) {
        this.statusHistories.add(history);
        history.assignBooking(this);
    }

    private void changeStatus(BookingStatus newStatus, User changedBy, String reason) {
        BookingStatus oldStatus = this.status;
        this.status = newStatus;

        BookingStatusHistory history = BookingStatusHistory.builder()
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .reason(reason)
                .build();

        this.addStatusHistory(history);
    }

    public void addAddOn(BookingAddOn addOn) {
        Objects.requireNonNull(addOn, "Dịch vụ không được để trống");

        boolean duplicated = this.addOns.stream().anyMatch(a -> a.getAddOnService()
                .getId()
                .equals(addOn.getAddOnService().getId()));

        if (duplicated) {
            throw new BookingValidationException("Dịch vụ đã được thêm vào đặt phòng.");
        }

        this.addOns.add(addOn);
        addOn.assignBooking(this);
    }

    public void removeAddOn(BookingAddOn addOn) {
        Objects.requireNonNull(addOn);

        this.addOns.remove(addOn);
        addOn.assignBooking(null);
    }

    public void confirm(User confirmedBy, Instant currentTime) {
        confirm(currentTime, confirmedBy);
    }

    public void confirmAutomatically(Instant currentTime) {
        confirm(currentTime, null);
    }

    private void confirm(Instant currentTime, User confirmedBy) {
        Objects.requireNonNull(currentTime, "Thời gian xác nhận không được để trống");

        if (!this.status.canConfirm()) {
            throw new InvalidBookingStateException("Chỉ có thể xác nhận đặt phòng khi đang ở trạng thái PENDING.");
        }

        if (this.expiredAt != null && currentTime.isAfter(this.expiredAt)) {
            throw new InvalidBookingStateException("Không thể xác nhận đặt phòng đã quá hạn thanh toán.");
        }

        this.expiredAt = null;
        changeStatus(BookingStatus.CONFIRMED, confirmedBy, "Xác nhận đặt phòng thành công");
    }

    public void recordPayment(long amount) {
        if (amount <= 0) {
            throw new BookingValidationException("Số tiền thanh toán phải lớn hơn 0.");
        }
        paidAmount = Math.addExact(paidAmount, amount);
    }

    public void assignAccessTokenHash(String accessTokenHash) {
        this.accessTokenHash = Objects.requireNonNull(accessTokenHash, "Mã bảo vệ đặt phòng không được để trống");
    }

    public void cancel(User cancelledBy, String reason, Instant cancelledAt) {
        Objects.requireNonNull(cancelledBy, "Người hủy không được để trống");
        Objects.requireNonNull(cancelledAt, "Thời gian hủy không được để trống");
        if (reason == null || reason.trim().isEmpty()) {
            throw new BookingValidationException("Lý do hủy không được để trống");
        }

        if (!this.status.canCancel()) {
            throw new InvalidBookingStateException("Trạng thái hiện tại (" + this.status + ") không cho phép hủy.");
        }

        this.cancelledAt = cancelledAt;
        this.expiredAt = null;
        changeStatus(BookingStatus.CANCELLED, cancelledBy, reason.trim());
    }

    public void checkIn(Room room, User receptionist) {
        Objects.requireNonNull(room, "Phòng không được để trống");
        Objects.requireNonNull(receptionist, "Lễ tân thực hiện check-in không được để trống");
        if (status != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException("Chỉ booking CONFIRMED mới có thể check-in.");
        }

        assignedRoom = room;
        changeStatus(BookingStatus.CHECKED_IN, receptionist, "Lễ tân check-in và gán phòng " + room.getRoomNumber());
    }

    public void checkOut(User receptionist, String note) {
        Objects.requireNonNull(receptionist, "Lễ tân thực hiện check-out không được để trống");
        if (status != BookingStatus.CHECKED_IN) {
            throw new InvalidBookingStateException("Chỉ booking CHECKED_IN mới có thể check-out.");
        }

        String reason = "Lễ tân check-out khách";
        if (note != null && !note.trim().isEmpty()) {
            reason = reason + ": " + note.trim();
        }
        changeStatus(BookingStatus.CHECKED_OUT, receptionist, reason);
    }

    public void extendTo(LocalDate newCheckOutDate, List<BookingNight> additionalNights, long additionalAmount) {
        Objects.requireNonNull(newCheckOutDate, "Ngày trả phòng mới không được để trống");
        if (status != BookingStatus.CONFIRMED && status != BookingStatus.CHECKED_IN) {
            throw new InvalidBookingStateException("Trạng thái hiện tại không cho phép gia hạn lưu trú.");
        }
        if (!newCheckOutDate.isAfter(checkOutDate)) {
            throw new BookingValidationException("Ngày trả phòng mới phải sau ngày trả phòng hiện tại.");
        }
        if (additionalAmount < 0) {
            throw new BookingValidationException("Số tiền gia hạn không hợp lệ.");
        }
        additionalNights.forEach(this::addNight);
        totalAmount = Math.addExact(totalAmount, additionalAmount);
        checkOutDate = newCheckOutDate;
    }

    public void expire(User systemUser, Instant currentTime) {
        Objects.requireNonNull(systemUser, "System User không được để trống");
        expire(currentTime, systemUser);
    }

    public void expireAutomatically(Instant currentTime) {
        expire(currentTime, null);
    }

    private void expire(Instant currentTime, User changedBy) {
        Objects.requireNonNull(currentTime, "Thời gian hiện tại không được để trống");

        if (!this.status.canExpire()) {
            throw new InvalidBookingStateException("Chỉ đặt phòng đang chờ thanh toán mới có thể hết hạn.");
        }

        if (this.expiredAt == null || currentTime.isBefore(this.expiredAt)) {
            throw new InvalidBookingStateException("Booking chưa đến thời hạn hết hạn.");
        }

        this.expiredAt = null;
        changeStatus(BookingStatus.EXPIRED, changedBy, "Hệ thống tự động hủy do quá hạn thanh toán");
    }

    public void applyTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void updatePendingDetails(
            RoomType roomType, LocalDate checkIn, LocalDate checkOut, int adults, int children) {
        if (status != BookingStatus.PENDING) {
            throw new InvalidBookingStateException("Chỉ đặt phòng đang chờ thanh toán mới được chỉnh sửa.");
        }
        this.roomType = Objects.requireNonNull(roomType);
        this.checkInDate = Objects.requireNonNull(checkIn);
        this.checkOutDate = Objects.requireNonNull(checkOut);
        this.adultCount = adults;
        this.childCount = children;
        this.guests.clear();
        this.addOns.clear();
        this.nights.clear();
    }
}
