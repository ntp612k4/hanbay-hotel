package com.tpanh.hotel.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.pricing.domain.HolidayCalendar;
import com.tpanh.hotel.pricing.exception.HolidayCalendarNotFoundException;
import com.tpanh.hotel.pricing.repository.HolidayCalendarRepository;
import com.tpanh.hotel.pricing.service.impl.HolidayCalendarManagementServiceImpl;
import com.tpanh.hotel.pricing.service.model.CreateHolidayCalendarCommand;
import com.tpanh.hotel.pricing.service.model.UpdateHolidayCalendarStatusCommand;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HolidayCalendarManagementServiceImplTest {

    private static final UUID HOLIDAY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Mock
    private HolidayCalendarRepository holidayCalendarRepository;

    @Test
    void createShouldNormalizeNameAndActivateHoliday() {
        when(holidayCalendarRepository.saveAndFlush(any(HolidayCalendar.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service()
                .create(new CreateHolidayCalendarCommand(
                        "  DIFF Festival  ", LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 8)));

        assertThat(result.name()).isEqualTo("DIFF Festival");
        assertThat(result.startDate()).isEqualTo(LocalDate.of(2027, 6, 1));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2027, 6, 8));
        assertThat(result.active()).isTrue();
    }

    @Test
    void createShouldRejectInvalidDateRange() {
        assertThatThrownBy(() -> service()
                        .create(new CreateHolidayCalendarCommand(
                                "Tet", LocalDate.of(2027, 1, 2), LocalDate.of(2027, 1, 1))))
                .isInstanceOf(RequestValidationException.class)
                .extracting("field")
                .isEqualTo("endDate");

        verify(holidayCalendarRepository, never()).saveAndFlush(any(HolidayCalendar.class));
    }

    @Test
    void updateStatusShouldDisableCalendarEntry() {
        HolidayCalendar holiday = HolidayCalendar.builder()
                .id(HOLIDAY_ID)
                .name("Tet Holiday")
                .startDate(LocalDate.of(2027, 1, 1))
                .endDate(LocalDate.of(2027, 1, 3))
                .active(true)
                .build();
        when(holidayCalendarRepository.findByIdForUpdate(HOLIDAY_ID)).thenReturn(Optional.of(holiday));
        when(holidayCalendarRepository.saveAndFlush(holiday)).thenReturn(holiday);

        var result = service().updateStatus(HOLIDAY_ID, new UpdateHolidayCalendarStatusCommand(false));

        assertThat(result.active()).isFalse();
        verify(holidayCalendarRepository).findByIdForUpdate(HOLIDAY_ID);
        verify(holidayCalendarRepository).saveAndFlush(holiday);
    }

    @Test
    void findByIdShouldThrowWhenCalendarDoesNotExist() {
        when(holidayCalendarRepository.findById(HOLIDAY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findById(HOLIDAY_ID))
                .isInstanceOf(HolidayCalendarNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo("HOLIDAY_CALENDAR_NOT_FOUND");
    }

    private HolidayCalendarManagementServiceImpl service() {
        return new HolidayCalendarManagementServiceImpl(holidayCalendarRepository);
    }
}
