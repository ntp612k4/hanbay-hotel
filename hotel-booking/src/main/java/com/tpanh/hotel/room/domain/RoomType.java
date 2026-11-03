package com.tpanh.hotel.room.domain;

import com.tpanh.hotel.common.domain.BaseEntity;
import com.tpanh.hotel.room.exception.RoomValidationException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "room_types")
public class RoomType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @EqualsAndHashCode.Include
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(name = "base_price", nullable = false)
    private long basePrice;

    @Column(name = "max_guests", nullable = false)
    private int maxGuests;

    @Column(name = "capacity_adults", nullable = false)
    private int capacityAdults;

    @Column(name = "capacity_children", nullable = false)
    private int capacityChildren;

    @Column(name = "bed_type", length = 100)
    private String bedType;

    @Column(name = "area_m2", precision = 6, scale = 2)
    private BigDecimal areaM2;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private RoomTypeStatus status = RoomTypeStatus.ACTIVE;

    /*
     * room_types (1) ---- (N) room_type_images
     */
    @Builder.Default
    @OneToMany(mappedBy = "roomType", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomTypeImage> roomTypeImages = new ArrayList<>();

    /*
     * room_types (N) ---- (N) amenities
     *
     * thông qua bảng:
     * room_type_amenities
     */
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "room_type_amenities",
            joinColumns = @JoinColumn(name = "room_type_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    private Set<Amenity> amenities = new LinkedHashSet<>();

    public void addImage(RoomTypeImage image) {
        if (image == null) {
            return;
        }

        roomTypeImages.add(image);
        image.assignRoomType(this);
    }

    public void removeImage(RoomTypeImage image) {
        if (image == null) {
            return;
        }

        roomTypeImages.remove(image);
        image.assignRoomType(null);
    }

    @PrePersist
    @PreUpdate
    private void validateInvariants() {
        if (maxGuests <= 0) {
            throw new RoomValidationException("Số khách tối đa phải lớn hơn 0.");
        }
        if (capacityAdults <= 0 || capacityChildren < 0) {
            throw new RoomValidationException("Sức chứa người lớn/trẻ em không hợp lệ.");
        }
        if (basePrice < 0) {
            throw new RoomValidationException("Giá cơ bản không được âm.");
        }
    }

    public void updateDetails(
            String code,
            String name,
            String description,
            String shortDescription,
            long basePrice,
            int maxGuests,
            int capacityAdults,
            int capacityChildren,
            String bedType,
            BigDecimal areaM2,
            RoomTypeStatus status,
            List<String> images,
            Set<Amenity> amenities) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.shortDescription = shortDescription;
        this.basePrice = basePrice;
        this.maxGuests = maxGuests;
        this.capacityAdults = capacityAdults;
        this.capacityChildren = capacityChildren;
        this.bedType = bedType;
        this.areaM2 = areaM2;
        this.status = status;
        this.thumbnailUrl = images.isEmpty() ? null : images.get(0);
        this.roomTypeImages.clear();
        for (int i = 0; i < images.size(); i++)
            addImage(
                    RoomTypeImage.builder().imageUrl(images.get(i)).sortOrder(i).build());
        this.amenities.clear();
        this.amenities.addAll(amenities);
        validateInvariants();
    }

    public boolean canAccommodate(int adults, int children) {
        if (adults < 0 || children < 0) return false;
        return adults <= capacityAdults && children <= capacityChildren && adults + children <= maxGuests;
    }
}
