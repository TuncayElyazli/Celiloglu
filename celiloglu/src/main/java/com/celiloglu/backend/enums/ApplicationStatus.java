package com.celiloglu.backend.enums;

public enum ApplicationStatus {
    GÖZLƏYİR("Gözləyir"),
    BAXILIR("Baxılır"),
    TƏSDİQLƏNDİ("Təsdiqləndi"),
    İMTİNA_EDİLDİ("İmtina edildi");

    private final String displayName;

    ApplicationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
