package com.zelia.agenda.domain.user;

import com.zelia.agenda.domain.agenda.ClassTime;
import com.zelia.agenda.domain.agenda.Period;
import com.zelia.agenda.domain.agenda.dto.CreateAgendaDTO;
import com.zelia.agenda.domain.schoolClass.dto.CreateClassDTO;
import com.zelia.agenda.domain.user.dto.CreateUserDTO;
import com.zelia.agenda.domain.user.dto.ForgotPasswordDTO;
import com.zelia.agenda.domain.user.dto.LoginRequestDTO;
import com.zelia.agenda.domain.user.dto.ResetPasswordDTO;
import com.zelia.agenda.domain.user.dto.UpdateUserDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static List<String> fields(Object dto) {
        return VALIDATOR.validate(dto).stream()
                .map(v -> v.getPropertyPath().toString())
                .toList();
    }

    @Test
    void validCreateUserHasNoErrors() {
        CreateUserDTO dto = new CreateUserDTO("Ana", "ana@exemplo.com", "senha12345", UserType.PROFESSOR);
        assertThat(fields(dto)).isEmpty();
    }

    @Test
    void createUserWithShortPasswordFlagsPasswordField() {
        CreateUserDTO dto = new CreateUserDTO("Ana", "ana@exemplo.com", "1234567", UserType.PROFESSOR);
        assertThat(fields(dto)).containsExactly("password");
    }

    @Test
    void createUserWithoutTypeFlagsUserTypeField() {
        CreateUserDTO dto = new CreateUserDTO("Ana", "ana@exemplo.com", "senha12345", null);
        assertThat(fields(dto)).containsExactly("userType");
    }

    @Test
    void updateUserWithBlankNameFlagsNameField() {
        UpdateUserDTO dto = new UpdateUserDTO("  ", "ana@exemplo.com", UserType.ADMIN);
        assertThat(fields(dto)).containsExactly("name");
    }

    @Test
    void loginWithInvalidEmailFlagsEmailField() {
        LoginRequestDTO dto = new LoginRequestDTO("nao-e-email", "senha");
        assertThat(fields(dto)).containsExactly("email");
    }

    @Test
    void forgotPasswordWithInvalidEmailFlagsEmailField() {
        assertThat(fields(new ForgotPasswordDTO("x"))).containsExactly("email");
    }

    @Test
    void validResetPasswordHasNoErrors() {
        assertThat(fields(new ResetPasswordDTO("token", "nova-senha-123"))).isEmpty();
    }

    @Test
    void resetPasswordWithBlankTokenAndShortPassword() {
        assertThat(fields(new ResetPasswordDTO("", "123")))
                .containsExactlyInAnyOrder("token", "newPassword");
    }

    @Test
    void createClassWithoutYearFlagsYearField() {
        assertThat(fields(new CreateClassDTO("", "I"))).containsExactly("year");
    }

    @Test
    void createAgendaWithoutRequiredFieldsFlagsAll() {
        CreateAgendaDTO dto = new CreateAgendaDTO(null, null, null, null, null);
        assertThat(fields(dto)).containsExactlyInAnyOrder("schoolClassId", "period", "classTime", "date");
    }

    @Test
    void validCreateAgendaHasNoErrors() {
        CreateAgendaDTO dto = new CreateAgendaDTO(1L, "obs", Period.MATUTINO, ClassTime.AULA_1, LocalDate.of(2026, 11, 18));
        assertThat(fields(dto)).isEmpty();
    }
}
