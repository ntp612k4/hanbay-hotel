package com.tpanh.hotel.pricing.repository;

import com.tpanh.hotel.pricing.domain.HolidayCalendar;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HolidayCalendarRepository
        extends JpaRepository<HolidayCalendar, UUID>, JpaSpecificationExecutor<HolidayCalendar> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select holiday from HolidayCalendar holiday where holiday.id = :id")
    java.util.Optional<HolidayCalendar> findByIdForUpdate(@Param("id") UUID id);

    @Query(
            """
            select holiday
            from HolidayCalendar holiday
            where holiday.active = true
              and holiday.startDate < :checkOut
              and holiday.endDate >= :checkIn
            """)
    List<HolidayCalendar> findActiveForStay(@Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut);

    @Query(
            """
            select holiday
            from HolidayCalendar holiday
            where (:active is null or holiday.active = :active)
              and (:keyword = '' or lower(holiday.name) like lower(concat('%', :keyword, '%')) escape '\\')
            """)
    Page<HolidayCalendar> findForManagement(
            @Param("keyword") String keyword, @Param("active") Boolean active, Pageable pageable);
}
