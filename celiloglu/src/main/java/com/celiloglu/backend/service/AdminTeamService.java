package com.celiloglu.backend.service;

import com.celiloglu.backend.dto.response.AdminTeamMemberDto;
import com.celiloglu.backend.dto.response.AdminTeamSummaryDto;
import com.celiloglu.backend.enums.Role;

import java.util.Optional;

public interface AdminTeamService {

    AdminTeamSummaryDto getTeamSummary();

    Optional<AdminTeamMemberDto> findActiveUserByExactUsername(String username);

    boolean assignRole(Long targetUserId, Role newRole, Long operatorId);
}
