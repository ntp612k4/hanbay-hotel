package com.tpanh.hotel.addon.config;

import com.tpanh.hotel.addon.domain.AddOnService;
import com.tpanh.hotel.addon.repository.AddOnServiceRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AddOnDataInitializer implements ApplicationRunner {

    private static final List<DefaultAddOn> DEFAULT_ADD_ONS = List.of(
            new DefaultAddOn(
                    "BREAKFAST_BUFFET",
                    "Buffet sáng",
                    "Buffet sáng tại nhà hàng khách sạn, giá áp dụng cho mỗi khách mỗi ngày.",
                    250_000),
            new DefaultAddOn(
                    "AIRPORT_PICKUP",
                    "Đón tại sân bay",
                    "Xe riêng đón một chiều từ Sân bay Quốc tế Đà Nẵng về khách sạn, tối đa 4 khách mỗi lượt.",
                    350_000),
            new DefaultAddOn(
                    "AIRPORT_DROPOFF",
                    "Tiễn ra sân bay",
                    "Xe riêng đưa một chiều từ khách sạn đến Sân bay Quốc tế Đà Nẵng, tối đa 4 khách mỗi lượt.",
                    350_000),
            new DefaultAddOn(
                    "EXTRA_BED",
                    "Giường phụ",
                    "Một giường phụ kèm bộ chăn ga, giá áp dụng cho mỗi giường mỗi đêm.",
                    500_000),
            new DefaultAddOn("LAUNDRY", "Giặt ủi", "Dịch vụ giặt và sấy quần áo, giá áp dụng cho mỗi kg.", 120_000),
            new DefaultAddOn(
                    "SPA_60_MINUTES",
                    "Trị liệu spa 60 phút",
                    "Liệu trình thư giãn toàn thân 60 phút tại spa của khách sạn, giá áp dụng cho mỗi khách.",
                    750_000),
            new DefaultAddOn(
                    "DA_NANG_CITY_TOUR",
                    "Tour khám phá Đà Nẵng",
                    "Tour nửa ngày tham quan các điểm nổi bật tại Đà Nẵng, giá áp dụng cho mỗi khách.",
                    650_000),
            new DefaultAddOn(
                    "HOI_AN_SHUTTLE",
                    "Xe đưa đón Hội An",
                    "Xe đưa đón khứ hồi giữa khách sạn và phố cổ Hội An theo lịch, giá áp dụng cho mỗi khách.",
                    300_000));

    private final AddOnServiceRepository addOnServiceRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Set<String> defaultCodes =
                DEFAULT_ADD_ONS.stream().map(DefaultAddOn::code).collect(Collectors.toSet());
        Set<String> existingCodes = addOnServiceRepository.findAllByCodeIn(defaultCodes).stream()
                .map(AddOnService::getCode)
                .collect(Collectors.toSet());

        List<AddOnService> missingAddOns = DEFAULT_ADD_ONS.stream()
                .filter(addOn -> !existingCodes.contains(addOn.code()))
                .map(DefaultAddOn::toEntity)
                .toList();

        if (!missingAddOns.isEmpty()) {
            addOnServiceRepository.saveAll(missingAddOns);
        }
    }

    private record DefaultAddOn(String code, String name, String description, long price) {

        private AddOnService toEntity() {
            return AddOnService.builder()
                    .code(code)
                    .name(name)
                    .description(description)
                    .price(price)
                    .active(true)
                    .build();
        }
    }
}
