package com.celiloglu.backend.enums;

public enum PaymentMethod {
    BANK_KÖÇÜRMƏSİ("Bank köçürməsi"),
    NAĞD("Kassa / Nağd qəbz"),
    KREDİT_XƏTTİ("Kredit xətti");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
