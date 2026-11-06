package com.celiloglu.backend.dto.response;

import com.celiloglu.backend.enums.OrderStatus;

public class OrderStatusStatDto {

    private OrderStatus status;
    private String displayName;
    private long count;
    private double percentage;
    private String barColorClass;
    private String badgeClass;

    public OrderStatusStatDto() {}

    public OrderStatusStatDto(OrderStatus status, String displayName, long count,
                              double percentage, String barColorClass, String badgeClass) {
        this.status = status;
        this.displayName = displayName;
        this.count = count;
        this.percentage = percentage;
        this.barColorClass = barColorClass;
        this.badgeClass = badgeClass;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public String getBarColorClass() {
        return barColorClass;
    }

    public void setBarColorClass(String barColorClass) {
        this.barColorClass = barColorClass;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public void setBadgeClass(String badgeClass) {
        this.badgeClass = badgeClass;
    }
}

