package com.celiloglu.backend.enums;

public enum Role {
    USER("Müştəri"),
    ADMIN("Admin"),
    SUPER_ADMIN("Super Admin"),
    CASHIER("Kassir"),
    WAREHOUSE_MANAGER("Anbar Müdiri");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
