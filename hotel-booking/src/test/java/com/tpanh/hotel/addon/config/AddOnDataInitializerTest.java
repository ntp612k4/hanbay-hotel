package com.tpanh.hotel.addon.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.addon.domain.AddOnService;
import com.tpanh.hotel.addon.repository.AddOnServiceRepository;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddOnDataInitializerTest {

    @Mock
    private AddOnServiceRepository addOnServiceRepository;

    @Test
    void runShouldOnlyCreateMissingAddOnsAndPreserveExistingManagerChanges() throws Exception {
        AddOnService managerUpdatedBreakfast = AddOnService.builder()
                .code("BREAKFAST_BUFFET")
                .name("Buffet theo mùa")
                .description("Nội dung do quản lý cập nhật")
                .price(999_000)
                .active(false)
                .build();
        when(addOnServiceRepository.findAllByCodeIn(anyCollection())).thenReturn(List.of(managerUpdatedBreakfast));

        new AddOnDataInitializer(addOnServiceRepository).run(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<AddOnService>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(addOnServiceRepository).saveAll(captor.capture());
        List<AddOnService> created =
                StreamSupport.stream(captor.getValue().spliterator(), false).toList();

        assertThat(created).hasSize(7).extracting(AddOnService::getCode).doesNotContain("BREAKFAST_BUFFET");
        assertThat(created)
                .filteredOn(addOn -> addOn.getCode().equals("AIRPORT_PICKUP"))
                .singleElement()
                .satisfies(addOn -> {
                    assertThat(addOn.getName()).isEqualTo("Đón tại sân bay");
                    assertThat(addOn.getPrice()).isEqualTo(350_000);
                    assertThat(addOn.isActive()).isTrue();
                });
        assertThat(managerUpdatedBreakfast.getName()).isEqualTo("Buffet theo mùa");
        assertThat(managerUpdatedBreakfast.getPrice()).isEqualTo(999_000);
        assertThat(managerUpdatedBreakfast.isActive()).isFalse();
    }
}
