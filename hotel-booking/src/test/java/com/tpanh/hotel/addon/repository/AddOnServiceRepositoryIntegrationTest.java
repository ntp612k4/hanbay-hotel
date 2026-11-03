package com.tpanh.hotel.addon.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tpanh.hotel.addon.domain.AddOnService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
class AddOnServiceRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg17");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.docker.compose.enabled", () -> false);
    }

    @Autowired
    private AddOnServiceRepository addOnServiceRepository;

    @Test
    void managementQueryShouldSearchCodeAndNameIgnoringCaseAndFilterStatus() {
        String marker = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        AddOnService active = AddOnService.create(marker + "_AIRPORT", marker + " Airport", null, 300_000);
        AddOnService inactive = AddOnService.create(marker + "_TOUR", marker + " Tour", null, 900_000);
        inactive.updateStatus(false);
        addOnServiceRepository.saveAllAndFlush(List.of(active, inactive));
        PageRequest byPriceDesc = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "price"));

        var allMatches = addOnServiceRepository.findForManagement(marker.toLowerCase(), null, byPriceDesc);
        var activeByName = addOnServiceRepository.findForManagement("airport", true, byPriceDesc);
        var inactiveByCode =
                addOnServiceRepository.findForManagement((marker + "_tour").toLowerCase(), false, byPriceDesc);

        assertThat(allMatches.getContent())
                .extracting(AddOnService::getCode)
                .containsExactly(inactive.getCode(), active.getCode());
        assertThat(activeByName.getContent()).extracting(AddOnService::getCode).contains(active.getCode());
        assertThat(inactiveByCode.getContent())
                .extracting(AddOnService::getCode)
                .containsExactly(inactive.getCode());
    }

    @Test
    void codeLookupShouldIgnoreCase() {
        String code = "UNIQUE_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        addOnServiceRepository.saveAndFlush(AddOnService.create(code, "Unique add-on", null, 1));

        assertThat(addOnServiceRepository.existsByCodeIgnoreCase(code.toLowerCase()))
                .isTrue();
    }

    @Test
    void managementQueryShouldTreatLikeWildcardsAsLiteralCharacters() {
        String marker = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        AddOnService literalMatch = AddOnService.create(marker + "_50_OFF", marker + " 50%_OFF", null, 1);
        AddOnService wildcardOnlyMatch = AddOnService.create(marker + "_50XOFF", marker + " 50XOFF", null, 1);
        addOnServiceRepository.saveAllAndFlush(List.of(literalMatch, wildcardOnlyMatch));

        var result =
                addOnServiceRepository.findForManagement("50\\%\\_OFF", null, PageRequest.of(0, 20, Sort.by("name")));

        assertThat(result.getContent()).extracting(AddOnService::getCode).containsExactly(literalMatch.getCode());
    }
}
