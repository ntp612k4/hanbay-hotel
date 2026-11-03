package com.tpanh.hotel.addon.service;

import com.tpanh.hotel.addon.dto.response.AddOnCatalogResponse;
import com.tpanh.hotel.addon.mapper.AddOnMapper;
import com.tpanh.hotel.addon.repository.AddOnServiceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddOnCatalogService {

    private final AddOnServiceRepository addOnServiceRepository;
    private final AddOnMapper addOnMapper;

    @Transactional(readOnly = true)
    public List<AddOnCatalogResponse> getActiveAddOns() {
        return addOnServiceRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(addOnMapper::toCatalogResponse)
                .toList();
    }
}
