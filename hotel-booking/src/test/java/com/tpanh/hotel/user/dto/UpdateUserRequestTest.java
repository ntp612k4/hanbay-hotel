package com.tpanh.hotel.user.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpanh.hotel.user.exception.ProfileValidationException;
import com.tpanh.hotel.user.service.model.UpdateUserCommand;
import org.junit.jupiter.api.Test;

class UpdateUserRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void distinguishesOmittedFieldFromExplicitNull() throws Exception {
        UpdateUserRequest request = objectMapper.readValue("{\"fullName\":null}", UpdateUserRequest.class);

        UpdateUserCommand command = request.toCommand();

        assertThat(command.fullName().present()).isTrue();
        assertThat(command.fullName().value()).isNull();
        assertThat(command.phone().present()).isFalse();
    }

    @Test
    void rejectsReadOnlyAndUnknownFields() throws Exception {
        UpdateUserRequest request = objectMapper.readValue(
                "{\"email\":\"other@example.com\",\"status\":\"ACTIVE\"}", UpdateUserRequest.class);

        assertThatThrownBy(request::toCommand)
                .isInstanceOfSatisfying(ProfileValidationException.class, exception -> assertThat(exception.getErrors())
                        .containsEntry("email", "Trường dữ liệu không được hỗ trợ")
                        .containsEntry("status", "Trường dữ liệu không được hỗ trợ"));
    }

    @Test
    void rejectsRoleEscalationField() throws Exception {
        UpdateUserRequest request = objectMapper.readValue("{\"roles\":[\"ADMIN\"]}", UpdateUserRequest.class);

        assertThatThrownBy(request::toCommand)
                .isInstanceOfSatisfying(ProfileValidationException.class, exception -> assertThat(exception.getErrors())
                        .containsKey("roles"));
    }
}
