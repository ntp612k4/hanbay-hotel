package com.tpanh.hotel.addon.repository;

import com.tpanh.hotel.addon.domain.AddOnService;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddOnServiceRepository extends JpaRepository<AddOnService, UUID> {
    List<AddOnService> findAllByActiveTrueOrderByNameAsc();

    List<AddOnService> findAllByIdInAndActiveTrue(Collection<UUID> ids);

    List<AddOnService> findAllByCodeIn(Collection<String> codes);

    boolean existsByCodeIgnoreCase(String code);

    @Query(
            """
            select service
            from AddOnService service
            where (:active is null or service.active = :active)
              and (:keyword = ''
                   or lower(service.code) like lower(concat('%', :keyword, '%')) escape '\\'
                   or lower(service.name) like lower(concat('%', :keyword, '%')) escape '\\')
            """)
    Page<AddOnService> findForManagement(
            @Param("keyword") String keyword, @Param("active") Boolean active, Pageable pageable);
}
