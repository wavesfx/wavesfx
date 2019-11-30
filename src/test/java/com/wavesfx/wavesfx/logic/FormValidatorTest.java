package com.wavesfx.wavesfx.logic;

import org.junit.Test;

import static org.junit.Assert.*;

public class FormValidatorTest {

    @Test
    public void isWellFormedPassword() {
        final var validLatinPassword = "Password111";
        final var invalidLatinPassword = "password";
        final var validCyrillicPassword = "Русскоеслово1";
        final var invalidCyrillicPassword = "pусскоеслово2";
        final var pattern = FormValidator.PASSWORD_PATTERN;

        assertTrue(FormValidator.isWellFormed(validLatinPassword, pattern));
        assertFalse(FormValidator.isWellFormed(invalidLatinPassword, pattern));
        assertTrue(FormValidator.isWellFormed(validCyrillicPassword, pattern));
        assertFalse(FormValidator.isWellFormed(invalidCyrillicPassword, pattern));
    }
}