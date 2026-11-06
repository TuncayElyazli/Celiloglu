package com.celiloglu.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetToolTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("12 simvoldan qısa şifrə rədd edilməlidir")
    void testPasswordTooShort() {
        assertThrows(IllegalArgumentException.class, () -> PasswordResetTool.validatePassword("Short1!"));
        assertThrows(IllegalArgumentException.class, () -> PasswordResetTool.validatePassword("12345678901"));
    }

    @Test
    @DisplayName("Boş və ya null şifrə rədd edilməlidir")
    void testPasswordEmptyOrNull() {
        assertThrows(IllegalArgumentException.class, () -> PasswordResetTool.validatePassword(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordResetTool.validatePassword(""));
    }

    @Test
    @DisplayName("72 UTF-8 baytdan böyük şifrə rədd edilməlidir")
    void testPasswordTooLongInUtf8Bytes() {
        // 73 simvol ASCII = 73 bayt
        String longPassword = "a".repeat(73);
        assertThrows(IllegalArgumentException.class, () -> PasswordResetTool.validatePassword(longPassword));

        // Çoxbaytlı simvollarla 72 baytı aşan hal (məs: 25 ədəd 3-baytlıq simvol = 75 bayt)
        String multiByteLong = "€".repeat(25);
        assertTrue(multiByteLong.getBytes(StandardCharsets.UTF_8).length > 72);
        assertThrows(IllegalArgumentException.class, () -> PasswordResetTool.validatePassword(multiByteLong));
    }

    @Test
    @DisplayName("12 simvol və 72 bayt daxilində olan şifrə qəbul edilməlidir və trim edilmir")
    void testValidPasswordWithoutTrimming() {
        String validPasswordWithSpaces = "  password12345  ";
        assertEquals(17, validPasswordWithSpaces.length());
        assertDoesNotThrow(() -> PasswordResetTool.validatePassword(validPasswordWithSpaces));

        // BCrypt hash düzgünlüyü
        String hash = encoder.encode(validPasswordWithSpaces);
        assertTrue(encoder.matches(validPasswordWithSpaces, hash));
        // Trim edilmiş variantı uyğun gəlməməlidir (çünki trim edilmir!)
        assertFalse(encoder.matches(validPasswordWithSpaces.trim(), hash));
    }
}
