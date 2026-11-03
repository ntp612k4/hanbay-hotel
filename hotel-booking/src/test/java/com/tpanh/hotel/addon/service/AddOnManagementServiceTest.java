package com.tpanh.hotel.addon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.addon.domain.AddOnService;
import com.tpanh.hotel.addon.exception.AddOnCodeAlreadyExistsException;
import com.tpanh.hotel.addon.exception.AddOnNotFoundException;
import com.tpanh.hotel.addon.repository.AddOnServiceRepository;
import com.tpanh.hotel.addon.service.impl.AddOnManagementServiceImpl;
import com.tpanh.hotel.addon.service.model.AddOnSearchCriteria;
import com.tpanh.hotel.addon.service.model.CreateAddOnCommand;
import com.tpanh.hotel.addon.service.model.UpdateAddOnCommand;
import com.tpanh.hotel.addon.service.model.UpdateAddOnStatusCommand;
import com.tpanh.hotel.common.exception.RequestValidationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class AddOnManagementServiceTest {

    @Mock
    private AddOnServiceRepository addOnServiceRepository;

    @Test
    void findAllShouldNormalizeCriteriaAndMapPage() {
        AddOnService addOn = AddOnService.create("AIRPORT_PICKUP", "Đón sân bay", null, 350_000);
        when(addOnServiceRepository.findForManagement(any(), any(), any()))
                .thenReturn(new PageImpl<>(
                        List.of(addOn), PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "price")), 21));

        var result = service().findAll(new AddOnSearchCriteria(1, 10, "  airport  ", true, "price", "DESC"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(addOnServiceRepository)
                .findForManagement(
                        org.mockito.ArgumentMatchers.eq("airport"),
                        org.mockito.ArgumentMatchers.eq(true),
                        pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("price").getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id").getDirection())
                .isEqualTo(Sort.Direction.ASC);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(21);
        assertThat(result.content()).extracting(item -> item.code()).containsExactly("AIRPORT_PICKUP");
    }

    @Test
    void findAllShouldRejectUnsupportedSortField() {
        assertThatThrownBy(() -> service().findAll(new AddOnSearchCriteria(0, 20, null, null, "id", "asc")))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("sort");

        verify(addOnServiceRepository, never()).findForManagement(any(), any(), any());
    }

    @Test
    void findAllShouldEscapeLikeWildcardsInUserSearch() {
        when(addOnServiceRepository.findForManagement(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        service().findAll(new AddOnSearchCriteria(0, 20, "50%_OFF\\VIP", null, "name", "asc"));

        verify(addOnServiceRepository)
                .findForManagement(
                        org.mockito.ArgumentMatchers.eq("50\\%\\_OFF\\\\VIP"),
                        org.mockito.ArgumentMatchers.isNull(),
                        any(Pageable.class));
    }

    @Test
    void findByIdShouldReturnInactiveAddOnForManagement() {
        UUID id = UUID.randomUUID();
        AddOnService addOn = AddOnService.builder()
                .id(id)
                .code("CITY_TOUR")
                .name("City tour")
                .price(650_000)
                .active(false)
                .build();
        when(addOnServiceRepository.findById(id)).thenReturn(Optional.of(addOn));

        var result = service().findById(id);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.active()).isFalse();
    }

    @Test
    void findByIdShouldThrowContractNotFoundError() {
        UUID id = UUID.randomUUID();
        when(addOnServiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findById(id))
                .isInstanceOf(AddOnNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo("ADD_ON_NOT_FOUND");
    }

    @Test
    void createShouldNormalizeInputAndActivateAddOn() {
        when(addOnServiceRepository.existsByCodeIgnoreCase("AIRPORT_PICKUP")).thenReturn(false);
        when(addOnServiceRepository.saveAndFlush(any(AddOnService.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service().create(new CreateAddOnCommand("  airport_pickup  ", "  Đón sân bay  ", "   ", 350_000L));

        ArgumentCaptor<AddOnService> entityCaptor = ArgumentCaptor.forClass(AddOnService.class);
        verify(addOnServiceRepository).saveAndFlush(entityCaptor.capture());
        AddOnService saved = entityCaptor.getValue();
        assertThat(saved.getCode()).isEqualTo("AIRPORT_PICKUP");
        assertThat(saved.getName()).isEqualTo("Đón sân bay");
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.isActive()).isTrue();
        assertThat(result.active()).isTrue();
    }

    @Test
    void createShouldRejectExistingCodeIgnoringCase() {
        when(addOnServiceRepository.existsByCodeIgnoreCase("CITY_TOUR")).thenReturn(true);

        assertThatThrownBy(() -> service().create(new CreateAddOnCommand("city_tour", "City tour", null, 1L)))
                .isInstanceOf(AddOnCodeAlreadyExistsException.class)
                .extracting("errorCode")
                .isEqualTo("ADD_ON_CODE_ALREADY_EXISTS");

        verify(addOnServiceRepository, never()).saveAndFlush(any());
    }

    @Test
    void createShouldTranslateDatabaseCodeRaceToConflict() {
        when(addOnServiceRepository.existsByCodeIgnoreCase("CITY_TOUR")).thenReturn(false);
        when(addOnServiceRepository.saveAndFlush(any(AddOnService.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "insert failed", new RuntimeException("duplicate add_on_services code constraint")));

        assertThatThrownBy(() -> service().create(new CreateAddOnCommand("CITY_TOUR", "City tour", null, 1L)))
                .isInstanceOf(AddOnCodeAlreadyExistsException.class);
    }

    @Test
    void createShouldNotHideUnrelatedDatabaseFailure() {
        DataIntegrityViolationException databaseFailure =
                new DataIntegrityViolationException("other constraint", new RuntimeException("price check failed"));
        when(addOnServiceRepository.existsByCodeIgnoreCase("CITY_TOUR")).thenReturn(false);
        when(addOnServiceRepository.saveAndFlush(any(AddOnService.class))).thenThrow(databaseFailure);

        assertThatThrownBy(() -> service().create(new CreateAddOnCommand("CITY_TOUR", "City tour", null, 1L)))
                .isSameAs(databaseFailure);
    }

    @Test
    void updateShouldChangeDetailsWithoutChangingCodeOrStatus() {
        UUID id = UUID.randomUUID();
        AddOnService addOn = AddOnService.create("CITY_TOUR", "City tour", "Cũ", 650_000);
        when(addOnServiceRepository.findById(id)).thenReturn(Optional.of(addOn));

        var result = service().update(id, new UpdateAddOnCommand("  City tour mới ", " Mới ", 700_000L));

        assertThat(result.code()).isEqualTo("CITY_TOUR");
        assertThat(result.name()).isEqualTo("City tour mới");
        assertThat(result.description()).isEqualTo("Mới");
        assertThat(result.price()).isEqualTo(700_000);
        assertThat(result.active()).isTrue();
        verify(addOnServiceRepository).saveAndFlush(addOn);
    }

    @Test
    void updateStatusShouldBeIdempotentAndNotChangeDetails() {
        UUID id = UUID.randomUUID();
        AddOnService addOn = AddOnService.create("CITY_TOUR", "City tour", null, 650_000);
        addOn.updateStatus(false);
        when(addOnServiceRepository.findById(id)).thenReturn(Optional.of(addOn));

        var result = service().updateStatus(id, new UpdateAddOnStatusCommand(false));

        assertThat(result.active()).isFalse();
        assertThat(result.code()).isEqualTo("CITY_TOUR");
        assertThat(result.price()).isEqualTo(650_000);
        verify(addOnServiceRepository).saveAndFlush(addOn);
    }

    @Test
    void serviceShouldDefendRequiredBusinessValuesWithoutHttpValidation() {
        assertThatThrownBy(() -> service().create(new CreateAddOnCommand("A", "Name", null, 1L)))
                .isInstanceOf(RequestValidationException.class);
        assertThatThrownBy(() -> service().create(new CreateAddOnCommand("VALID_CODE", "Name", null, -1L)))
                .isInstanceOf(RequestValidationException.class);
        assertThatThrownBy(() -> service().updateStatus(UUID.randomUUID(), new UpdateAddOnStatusCommand(null)))
                .isInstanceOf(RequestValidationException.class);

        verify(addOnServiceRepository, never()).saveAndFlush(any());
    }

    private AddOnManagementService service() {
        return new AddOnManagementServiceImpl(addOnServiceRepository);
    }
}
