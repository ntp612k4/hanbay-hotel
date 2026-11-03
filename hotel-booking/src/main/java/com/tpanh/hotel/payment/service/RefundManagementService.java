package com.tpanh.hotel.payment.service;

import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.payment.dto.request.ConfirmRefundRequest;
import com.tpanh.hotel.payment.dto.response.RefundRequestResponse;
import java.util.UUID;

public interface RefundManagementService {
    PageResponse<RefundRequestResponse> getPendingRefunds(int page, int size);

    RefundRequestResponse confirmRefund(UUID refundId, ConfirmRefundRequest request);
}
