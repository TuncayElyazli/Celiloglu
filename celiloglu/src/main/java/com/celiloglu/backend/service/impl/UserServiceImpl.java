package com.celiloglu.backend.service.impl;

import com.celiloglu.backend.dto.request.UserCreateRequestDto;
import com.celiloglu.backend.dto.request.UserUpdateRequestDto;
import com.celiloglu.backend.dto.response.UserResponseDto;
import com.celiloglu.backend.entity.User;
import com.celiloglu.backend.enums.Role;
import com.celiloglu.backend.repository.UserRepository;
import com.celiloglu.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException("Səhifələmə məlumatı boş ola bilməz!");
        }
        return userRepository.findAll(pageable)
                .map(this::mapToResponseDto);
    }

    @Override
    public UserResponseDto getUserById(Long id) {
        User user = findUser(id);
        checkUserModificationPermission(user);
        return mapToResponseDto(user);
    }

    @Override
    @Transactional
    public UserResponseDto createUser(UserCreateRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("İstifadəçi məlumatları boş ola bilməz!");
        }

        String username = validateAndNormalizeUsername(request.getUsername());
        String email = validateAndNormalizeEmail(request.getEmail());

        validateUsernameUniqueness(username, null);
        validateEmailUniqueness(email, null);
        validatePassword(request.getPassword());

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(sanitizeOptionalField(request.getFirstName(), "Ad", 255));
        user.setLastName(sanitizeOptionalField(request.getLastName(), "Soyad", 255));
        user.setPhone(sanitizeOptionalField(request.getPhone(), "Telefon nömrəsi", 50));
        user.setAddress(sanitizeOptionalField(request.getAddress(), "Ünvan", 255));
        user.setCompanyName(sanitizeOptionalField(request.getCompanyName(), "Şirkət adı", 255));
        user.setTin(sanitizeOptionalField(request.getTin(), "VÖEN", 50));

        user.setIsActive(true);
        user.setRole(Role.USER);
        user.setRoles(new HashSet<>(Collections.singleton(Role.USER)));

        User savedUser = userRepository.save(user);
        return mapToResponseDto(savedUser);
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(Long id, UserUpdateRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Redaktə məlumatları boş ola bilməz!");
        }

        User user = findUser(id);
        checkUserModificationPermission(user);

        String username = validateAndNormalizeUsername(request.getUsername());
        String email = validateAndNormalizeEmail(request.getEmail());

        validateUsernameUniqueness(username, id);
        validateEmailUniqueness(email, id);

        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(sanitizeOptionalField(request.getFirstName(), "Ad", 255));
        user.setLastName(sanitizeOptionalField(request.getLastName(), "Soyad", 255));
        user.setPhone(sanitizeOptionalField(request.getPhone(), "Telefon nömrəsi", 50));
        user.setAddress(sanitizeOptionalField(request.getAddress(), "Ünvan", 255));
        user.setCompanyName(sanitizeOptionalField(request.getCompanyName(), "Şirkət adı", 255));
        user.setTin(sanitizeOptionalField(request.getTin(), "VÖEN", 50));

        User updatedUser = userRepository.save(user);
        return mapToResponseDto(updatedUser);
    }

    @Override
    @Transactional
    public UserResponseDto setUserActive(Long id, boolean active) {
        User user = findUser(id);
        checkUserModificationPermission(user);
        if (!active && user.getRole() == Role.SUPER_ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("SUPER_ADMIN hesabı deaktiv edilə bilməz!");
        }
        user.setIsActive(active);
        User savedUser = userRepository.save(user);
        return mapToResponseDto(savedUser);
    }

    private void checkUserModificationPermission(User targetUser) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            boolean isSuperAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

            if (isAdmin && !isSuperAdmin) {
                if (targetUser != null && targetUser.getRole() != Role.USER) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Admin yalnız adi müştəri (USER) hesablarını idarə edə bilər!"
                    );
                }
            }
        }
    }

    private User findUser(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("İstifadəçi ID-si müsbət ədəd olmalıdır!");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("İstifadəçi tapılmadı! ID: " + id));
    }

    private String validateAndNormalizeUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("İstifadəçi adı boş ola bilməz!");
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("İstifadəçi adı maksimum 255 simvol ola bilər!");
        }
        return normalized;
    }

    private String validateAndNormalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("E-poçt ünvanı boş ola bilməz!");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("E-poçt ünvanı maksimum 255 simvol ola bilər!");
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Düzgün e-poçt formatı daxil edin!");
        }
        return normalized;
    }

    private void validateUsernameUniqueness(String username, Long currentUserId) {
        userRepository.findByUsernameIgnoreCase(username).ifPresent(existing -> {
            if (currentUserId == null || !existing.getId().equals(currentUserId)) {
                throw new IllegalArgumentException("Bu istifadəçi adı artıq istifadə olunur!");
            }
        });
    }

    private void validateEmailUniqueness(String email, Long currentUserId) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (currentUserId == null || !existing.getId().equals(currentUserId)) {
                throw new IllegalArgumentException("Bu e-poçt ünvanı artıq istifadə olunur!");
            }
        });
    }

    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Şifrə boş ola bilməz!");
        }
        if (password.length() < 12) {
            throw new IllegalArgumentException("Şifrə ən azı 12 simvoldan ibarət olmalıdır!");
        }
        byte[] bytes = password.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 72) {
            throw new IllegalArgumentException("Şifrə maksimum 72 bayt (UTF-8) ola bilər!");
        }
    }

    private String sanitizeOptionalField(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " maksimum " + maxLength + " simvol ola bilər!");
        }
        return trimmed;
    }

    private UserResponseDto mapToResponseDto(User user) {
        if (user == null) {
            return null;
        }

        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setCompanyName(user.getCompanyName());
        dto.setTin(user.getTin());
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());

        if (user.getRole() != null) {
            dto.setRole(user.getRole().name());
            dto.setRoleDisplayName(user.getRole().getDisplayName());
        } else {
            dto.setRoleDisplayName("—");
        }

        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            dto.setRoles(user.getRoles().stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet()));
        }

        return dto;
    }
}
