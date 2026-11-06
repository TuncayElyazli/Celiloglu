package com.celiloglu.backend.service.impl;

import com.celiloglu.backend.dto.response.AdminTeamMemberDto;
import com.celiloglu.backend.dto.response.AdminTeamSummaryDto;
import com.celiloglu.backend.entity.User;
import com.celiloglu.backend.enums.Role;
import com.celiloglu.backend.repository.UserRepository;
import com.celiloglu.backend.service.AdminTeamService;
import com.celiloglu.backend.service.UserSessionManagementService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminTeamServiceImpl implements AdminTeamService {

    private static final Logger log = LoggerFactory.getLogger(AdminTeamServiceImpl.class);
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT.TEAM_RBAC");

    private static final List<Role> STAFF_ROLES = List.of(
            Role.SUPER_ADMIN,
            Role.ADMIN,
            Role.CASHIER,
            Role.WAREHOUSE_MANAGER);

    private final UserRepository userRepository;
    private final UserSessionManagementService userSessionManagementService;

    public AdminTeamServiceImpl(UserRepository userRepository,
            UserSessionManagementService userSessionManagementService) {
        this.userRepository = userRepository;
        this.userSessionManagementService = userSessionManagementService;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminTeamSummaryDto getTeamSummary() {
        long superAdminCount = userRepository.countByRoleAndIsActiveTrue(Role.SUPER_ADMIN);
        long adminCount = userRepository.countByRoleAndIsActiveTrue(Role.ADMIN);
        long cashierCount = userRepository.countByRoleAndIsActiveTrue(Role.CASHIER);
        long warehouseCount = userRepository.countByRoleAndIsActiveTrue(Role.WAREHOUSE_MANAGER);

        List<User> staffUsers = userRepository.findByRoleIn(
                STAFF_ROLES,
                Sort.by(Sort.Order.asc("role"), Sort.Order.desc("id")));

        List<AdminTeamMemberDto> members = staffUsers.stream()
                .map(this::mapToMemberDto)
                .collect(Collectors.toList());

        return new AdminTeamSummaryDto(superAdminCount, adminCount, cashierCount, warehouseCount, members);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdminTeamMemberDto> findActiveUserByExactUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }

        return userRepository.findByUsernameIgnoreCase(username.trim())
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .map(this::mapToMemberDto);
    }

    @Override
    @Transactional
    public boolean assignRole(Long targetUserId, Role newRole, Long operatorId) {
        if (operatorId == null || operatorId <= 0) {
            throw new AccessDeniedException("Autentifikasiya olunmuş operator tapılmadı.");
        }

        // 1. Operatorun bazada aktual aktiv SUPER_ADMIN olduğunu yoxla
        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new AccessDeniedException("Operator hesabı tapılmadı."));

        if (!Boolean.TRUE.equals(operator.getIsActive()) || operator.getRole() != Role.SUPER_ADMIN) {
            throw new AccessDeniedException("Yalnız aktiv SUPER_ADMIN işçi rolunu təyin edə bilər.");
        }

        // 2. Yeni rolun validasiyası
        if (newRole == null) {
            throw new IllegalArgumentException("Təyin ediləcək yeni rol seçilməlidir.");
        }
        if (newRole == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Bu versiyada SUPER_ADMIN rolu təyin edilə bilməz.");
        }

        // 3. Hədəf istifadəçinin tapılması və aktual rolunun oxunması (pessimistic lock
        // ilə)
        if (targetUserId == null || targetUserId <= 0) {
            throw new IllegalArgumentException("Düzgün istifadəçi ID-si qeyd edilməlidir.");
        }
        User targetUser = userRepository.findByIdWithLock(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("İstifadəçi tapılmadı! ID: " + targetUserId));

        Role currentRole = targetUser.getRole();

        // 4. Mövcud SUPER_ADMIN-in rolunu dəyişmək qadağandır
        if (currentRole == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Mövcud SUPER_ADMIN-in rolunu dəyişmək mümkün deyil.");
        }

        // 5. Eyni rol yenidən seçilərsə, dəyişiklik edilmir
        if (currentRole == newRole) {
            log.info("İstifadəçinin (@{}) rolu artıq {} olduğundan heç bir dəyişiklik edilmədi.",
                    targetUser.getUsername(), newRole);
            return false;
        }

        // 6. Rolun yenilənməsi
        Role oldRole = currentRole;
        targetUser.setRole(newRole);
        if (targetUser.getRoles() != null) {
            targetUser.getRoles().clear();
            targetUser.getRoles().add(newRole);
        }
        userRepository.save(targetUser);

        // 7. Hədəf hesabın əvvəlki sessiyalarının etibarsız edilməsi
        userSessionManagementService.invalidateUserSessions(targetUser.getId(), targetUser.getUsername());

        // 8. Audit qeydi: operator ID, hədəf ID, əvvəlki və yeni rol
        AUDIT_LOG.info(
                "AUDIT_ROLE_CHANGE: OperatorId={}, TargetUserId={}, TargetUsername='{}', OldRole={}, NewRole={}, Timestamp={}",
                operatorId, targetUser.getId(), targetUser.getUsername(), oldRole, newRole, LocalDateTime.now());

        log.info("İstifadəçinin (@{}) rolu uğurla {} -> {} olaraq dəyişdirildi (OperatorId={}).",
                targetUser.getUsername(), oldRole, newRole, operatorId);

        return true;
    }

    private AdminTeamMemberDto mapToMemberDto(User user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        if (fullName.isEmpty()) {
            fullName = user.getUsername();
        }

        return new AdminTeamMemberDto(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                fullName,
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt());
    }
}
