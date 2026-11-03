package com.tpanh.hotel.pricing.repository;

import com.tpanh.hotel.pricing.domain.PriceRule;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceRuleRepository extends JpaRepository<PriceRule, UUID>, JpaSpecificationExecutor<PriceRule> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PriceRule p WHERE p.id = :id")
    Optional<PriceRule> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT p FROM PriceRule p " + "WHERE p.roomType.id = :roomTypeId "
            + "AND p.active = true "
            + "AND p.startDate < :checkOut "
            + "AND p.endDate >= :checkIn")
    List<PriceRule> findActiveRulesForStay(
            @Param("roomTypeId") UUID roomTypeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut);
}
