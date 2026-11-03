package com.tpanh.hotel.reporting.service;

import com.tpanh.hotel.reporting.dto.request.DashboardReportRequest;
import com.tpanh.hotel.reporting.dto.response.DashboardReportResponse;

public interface DashboardReportService {

    DashboardReportResponse getDashboardReport(DashboardReportRequest request);
}
