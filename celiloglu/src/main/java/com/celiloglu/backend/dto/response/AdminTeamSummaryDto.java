package com.celiloglu.backend.dto.response;

import java.util.ArrayList;
import java.util.List;

public class AdminTeamSummaryDto {

    private long superAdminCount;
    private long adminCount;
    private long cashierCount;
    private long warehouseCount;
    private List<AdminTeamMemberDto> members = new ArrayList<>();

    public AdminTeamSummaryDto() {
    }

    public AdminTeamSummaryDto(long superAdminCount, long adminCount, long cashierCount,
            long warehouseCount, List<AdminTeamMemberDto> members) {
        this.superAdminCount = superAdminCount;
        this.adminCount = adminCount;
        this.cashierCount = cashierCount;
        this.warehouseCount = warehouseCount;
        this.members = members != null ? members : new ArrayList<>();
    }

    public long getSuperAdminCount() {
        return superAdminCount;
    }

    public void setSuperAdminCount(long superAdminCount) {
        this.superAdminCount = superAdminCount;
    }

    public long getAdminCount() {
        return adminCount;
    }

    public void setAdminCount(long adminCount) {
        this.adminCount = adminCount;
    }

    public long getCashierCount() {
        return cashierCount;
    }

    public void setCashierCount(long cashierCount) {
        this.cashierCount = cashierCount;
    }

    public long getWarehouseCount() {
        return warehouseCount;
    }

    public void setWarehouseCount(long warehouseCount) {
        this.warehouseCount = warehouseCount;
    }

    public List<AdminTeamMemberDto> getMembers() {
        return members;
    }

    public void setMembers(List<AdminTeamMemberDto> members) {
        this.members = members != null ? members : new ArrayList<>();
    }
}
