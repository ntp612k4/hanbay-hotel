package com.tpanh.hotel.frontdesk.service;

import com.tpanh.hotel.common.response.PageResponse;
import com.tpanh.hotel.frontdesk.dto.response.TodayArrivalResponse;

public interface FrontDeskService {

    PageResponse<TodayArrivalResponse> getTodayArrivals(int page, int size);
}
