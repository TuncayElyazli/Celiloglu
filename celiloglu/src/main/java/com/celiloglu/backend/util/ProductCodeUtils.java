package com.celiloglu.backend.util;

import java.util.Locale;

public final class ProductCodeUtils {

    private ProductCodeUtils() {
    }

    /**
     * Məhsul kodunu vahid böyük hərf standartına normallaşdırır.
     * Null-safe qaydada işləyir: null daxil edildikdə null qaytarır.
     * Baş və son boşluqları təmizləyir və toUpperCase(Locale.ROOT) tətbiq edir.
     * Daxili tireləri, rəqəmləri və əvvəlindəki sıfırları dəyişmir.
     *
     * @param code Məhsul kodu
     * @return Normallaşdırılmış məhsul kodu və ya null
     */
    public static String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.toUpperCase(Locale.ROOT);
    }
}

