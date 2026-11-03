package com.tpanh.hotel.seed.config;

import com.tpanh.hotel.room.domain.Amenity;
import com.tpanh.hotel.room.repository.AmenityRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
@RequiredArgsConstructor
public class AmenityDataInitializer implements ApplicationRunner {

    private static final List<DefaultAmenity> DEFAULT_AMENITIES = List.of(
            new DefaultAmenity("Wi-Fi", "wifi"),
            new DefaultAmenity("Bể bơi", "pool"),
            new DefaultAmenity("TV", "tv"),
            new DefaultAmenity("Máy điều hòa", "ac"),
            new DefaultAmenity("Đỗ xe miễn phí", "parking"),
            new DefaultAmenity("Nhà hàng", "restaurant"));

    private final AmenityRepository amenityRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (amenityRepository.count() > 0) return;

        List<Amenity> amenities = DEFAULT_AMENITIES.stream()
                .map(a -> {
                    Amenity ent = new Amenity();
                    ent.getClass(); // keep lombok/no-arg visibility
                    try {
                        java.lang.reflect.Field idField = Amenity.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(ent, UUID.randomUUID());
                        java.lang.reflect.Field nameField = Amenity.class.getDeclaredField("name");
                        nameField.setAccessible(true);
                        nameField.set(ent, a.name());
                        java.lang.reflect.Field iconField = Amenity.class.getDeclaredField("icon");
                        iconField.setAccessible(true);
                        iconField.set(ent, a.icon());
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                    return ent;
                })
                .toList();

        amenityRepository.saveAll(amenities);
    }

    private record DefaultAmenity(String name, String icon) {}
}
