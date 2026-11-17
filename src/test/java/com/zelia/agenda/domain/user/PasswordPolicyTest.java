package com.zelia.agenda.domain.user;

import com.zelia.agenda.exceptions.InvalidPasswordException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void acceptsPasswordWithEightCharacters() {
        assertThatCode(() -> PasswordPolicy.validate("12345678")).doesNotThrowAnyException();
    }

    @Test
    void rejectsPasswordShorterThanEightCharacters() {
        assertThatThrownBy(() -> PasswordPolicy.validate("1234567"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("8 caracteres");
    }

    @Test
    void rejectsNullPassword() {
        assertThatThrownBy(() -> PasswordPolicy.validate(null))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void acceptsPasswordWithExactly72Bytes() {
        assertThatCode(() -> PasswordPolicy.validate("a".repeat(72))).doesNotThrowAnyException();
    }

    @Test
    void rejectsPasswordLongerThan72Bytes() {
        assertThatThrownBy(() -> PasswordPolicy.validate("a".repeat(73)))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("excede");
    }

    @Test
    void countsUtf8BytesNotCharacters() {
        // "ç" takes 2 bytes in UTF-8: 37 characters = 74 bytes, above the limit
        String senha = "\u00e7".repeat(37);
        assertThatThrownBy(() -> PasswordPolicy.validate(senha))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("excede");
    }
}
