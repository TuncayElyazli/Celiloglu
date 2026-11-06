package com.celiloglu.backend.enums;

public enum PaymentStatus {
    TAM_ÖDƏNİLMİŞ("Tam Ödənilmiş"),
    QİSMƏN_ÖDƏNİLMİŞ("Qismən Ödənilmiş"),
    ÖDƏNİŞ_GÖZLƏYİR("Ödəniş Gözləyən");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
