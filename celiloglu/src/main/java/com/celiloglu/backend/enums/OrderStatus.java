package com.celiloglu.backend.enums;

public enum OrderStatus {
    GÖZLƏYİR("Gözləyir"),
    HAZIRLANIR("Hazırlanır"),
    KARQOYA_VERİLDİ("Karqoya verildi"),
    ÇATDIRILDI("Çatdırıldı"),
    LƏĞV_EDİLDİ("Ləğv edildi");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
