package com.tpanh.hotel.ai.service;

import com.tpanh.hotel.addon.dto.response.AddOnCatalogResponse;
import com.tpanh.hotel.addon.service.AddOnCatalogService;
import com.tpanh.hotel.booking.domain.BookingStatus;
import com.tpanh.hotel.booking.dto.response.BookingHistoryItemResponse;
import com.tpanh.hotel.booking.dto.response.CancellationEligibilityResponse;
import com.tpanh.hotel.booking.service.BookingService;
import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.payment.dto.response.PaymentStatusResponse;
import com.tpanh.hotel.payment.service.PaymentService;
import com.tpanh.hotel.pricing.dto.response.CalculateStayPriceResponse;
import com.tpanh.hotel.pricing.mapper.PricingMapper;
import com.tpanh.hotel.pricing.service.PricingService;
import com.tpanh.hotel.room.dto.RoomTypeDetailResponse;
import com.tpanh.hotel.room.dto.response.AvailableRoomTypeResponse;
import com.tpanh.hotel.room.service.AvailableRoomTypeSearchService;
import com.tpanh.hotel.room.service.RoomTypeService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotelAiTools {

    private static final int MAX_PAGE_SIZE = 20;

    private final AvailableRoomTypeSearchService availableRoomTypeSearchService;
    private final PricingService pricingService;
    private final PricingMapper pricingMapper;
    private final RoomTypeService roomTypeService;
    private final AddOnCatalogService addOnCatalogService;
    private final BookingService bookingService;
    private final PaymentService paymentService;

    @Tool(
            name = "search_available_rooms",
            description = "Tìm các hạng phòng còn trống theo ngày lưu trú và số lượng khách. Ngày dùng ISO yyyy-MM-dd.")
    public PageResponse<AvailableRoomTypeResponse> searchAvailableRooms(
            @ToolParam(description = "Ngày nhận phòng, định dạng yyyy-MM-dd") LocalDate checkIn,
            @ToolParam(description = "Ngày trả phòng, định dạng yyyy-MM-dd") LocalDate checkOut,
            @ToolParam(description = "Số người lớn, tối thiểu 1") int adults,
            @ToolParam(description = "Số trẻ em, tối thiểu 0") int children) {
        return availableRoomTypeSearchService.search(checkIn, checkOut, adults, children, null, 0, MAX_PAGE_SIZE);
    }

    @Tool(
            name = "calculate_stay_price",
            description = "Tính giá chính xác theo từng đêm cho một hạng phòng và khoảng ngày lưu trú.")
    public CalculateStayPriceResponse calculateStayPrice(
            @ToolParam(description = "UUID của hạng phòng") UUID roomTypeId,
            @ToolParam(description = "Ngày nhận phòng, định dạng yyyy-MM-dd") LocalDate checkIn,
            @ToolParam(description = "Ngày trả phòng, định dạng yyyy-MM-dd") LocalDate checkOut) {
        return pricingMapper.toResponse(pricingService.calculateStayPrice(roomTypeId, checkIn, checkOut));
    }

    @Tool(
            name = "get_room_type_details",
            description =
                    "Lấy mô tả, sức chứa, ảnh và tiện nghi theo tên hạng phòng mà người dùng nhìn thấy; không yêu cầu người dùng cung cấp UUID.")
    public RoomTypeDetailResponse getRoomTypeDetails(
            @ToolParam(description = "Tên chính xác từ kết quả tìm phòng, ví dụ Junior Suite") String roomTypeName) {
        return roomTypeService.getDetailByName(roomTypeName);
    }

    @Tool(name = "list_active_addons", description = "Liệt kê các dịch vụ bổ sung đang hoạt động và có thể đặt.")
    public List<AddOnCatalogResponse> listActiveAddons() {
        return addOnCatalogService.getActiveAddOns();
    }

    @Tool(
            name = "get_my_bookings",
            description = "Lấy danh sách booking của tài khoản đang đăng nhập. Chỉ dùng dữ liệu của chính người dùng.")
    public PageResponse<BookingHistoryItemResponse> getMyBookings(
            @ToolParam(description = "Trạng thái booking cần lọc; bỏ trống để lấy tất cả", required = false)
                    BookingStatus status) {
        return bookingService.getCurrentUserBookingHistory(0, MAX_PAGE_SIZE, status);
    }

    @Tool(
            name = "get_booking_details",
            description = "Lấy chi tiết một booking thuộc tài khoản đang đăng nhập bằng mã booking.")
    public BookingHistoryItemResponse getBookingDetails(
            @ToolParam(description = "Mã booking, ví dụ HAN-ABC123") String bookingCode) {
        return bookingService.getCurrentUserBooking(bookingCode);
    }

    @Tool(
            name = "get_payment_status",
            description = "Lấy trạng thái thanh toán theo UUID payment. Payment phải thuộc tài khoản đang đăng nhập.")
    public PaymentStatusResponse getPaymentStatus(
            @ToolParam(description = "UUID payment lấy từ booking của người dùng") UUID paymentId) {
        return paymentService.getPaymentStatus(paymentId, null);
    }

    @Tool(
            name = "check_cancellation_eligibility",
            description =
                    "Kiểm tra booking của người dùng có thể hủy hay không và số tiền dự kiến được hoàn. Không thực hiện hủy.")
    public CancellationEligibilityResponse checkCancellationEligibility(
            @ToolParam(description = "Mã booking cần kiểm tra") String bookingCode) {
        return bookingService.getCancellationEligibility(bookingCode);
    }
}
