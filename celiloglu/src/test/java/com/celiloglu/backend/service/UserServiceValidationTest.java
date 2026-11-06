package com.celiloglu.backend.service;

import com.celiloglu.backend.dto.request.UserCreateRequestDto;
import com.celiloglu.backend.dto.request.UserUpdateRequestDto;
import com.celiloglu.backend.dto.response.UserResponseDto;
import com.celiloglu.backend.entity.Cart;
import com.celiloglu.backend.entity.Order;
import com.celiloglu.backend.entity.User;
import com.celiloglu.backend.enums.Role;
import com.celiloglu.backend.repository.UserRepository;
import com.celiloglu.backend.service.impl.UserServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceValidationTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private UserServiceImpl userService;

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserServiceImpl(userRepository, passwordEncoder);

        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setUsername("testuser");
        activeUser.setFirstName("Əli");
        activeUser.setLastName("Məmmədov");
        activeUser.setEmail("ali@example.com");
        activeUser.setPassword(passwordEncoder.encode("existingSecret123"));
        activeUser.setPhone("+994501234567");
        activeUser.setAddress("Bakı ş., Nizami r.");
        activeUser.setCompanyName("MMC MMC");
        activeUser.setTin("1234567890");
        activeUser.setIsActive(true);
        activeUser.setCreatedAt(LocalDateTime.now());
        activeUser.setRole(Role.USER);
        activeUser.setRoles(Set.of(Role.USER));
        activeUser.setCart(new Cart());
        activeUser.setOrders(List.of(new Order()));

        inactiveUser = new User();
        inactiveUser.setId(2L);
        inactiveUser.setUsername("inactive_user");
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setIsActive(false);
        inactiveUser.setRole(Role.ADMIN);
        inactiveUser.setRoles(Set.of(Role.ADMIN, Role.USER));
    }

    // --- getUserById Tests ---

    @Test
    @DisplayName("getUserById - Uğurlu tapılma və DTO çevrilməsi")
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        UserResponseDto dto = userService.getUserById(1L);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("testuser", dto.getUsername());
        assertEquals("Əli", dto.getFirstName());
        assertEquals("Məmmədov", dto.getLastName());
        assertEquals("ali@example.com", dto.getEmail());
        assertEquals("+994501234567", dto.getPhone());
        assertEquals("Bakı ş., Nizami r.", dto.getAddress());
        assertEquals("MMC MMC", dto.getCompanyName());
        assertEquals("1234567890", dto.getTin());
        assertTrue(dto.getIsActive());
        assertEquals("USER", dto.getRole());
        assertTrue(dto.getRoles().contains("USER"));
        assertNotNull(dto.getCreatedAt());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getUserById - Null ID üçün IllegalArgumentException verməlidir")
    void getUserById_NullId_ThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.getUserById(null));
        assertTrue(ex.getMessage().contains("müsbət"));
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("getUserById - 0 və ya mənfi ID üçün IllegalArgumentException verməlidir")
    void getUserById_ZeroOrNegativeId_ThrowsException() {
        IllegalArgumentException exZero = assertThrows(IllegalArgumentException.class,
                () -> userService.getUserById(0L));
        assertTrue(exZero.getMessage().contains("müsbət"));

        IllegalArgumentException exNegative = assertThrows(IllegalArgumentException.class,
                () -> userService.getUserById(-5L));
        assertTrue(exNegative.getMessage().contains("müsbət"));

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("getUserById - Tapılmayan ID üçün EntityNotFoundException verməlidir")
    void getUserById_NotFound_ThrowsEntityNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> userService.getUserById(99L));
        assertTrue(ex.getMessage().contains("İstifadəçi tapılmadı"));
        verify(userRepository, times(1)).findById(99L);
    }

    // --- getAllUsers Tests ---

    @Test
    @DisplayName("getAllUsers - Aktiv və deaktiv istifadəçilərin hər ikisini səhifələmə ilə qaytarmalıdır")
    void getAllUsers_ReturnsBothActiveAndInactive() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(activeUser, inactiveUser), pageable, 2);

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<UserResponseDto> result = userService.getAllUsers(pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("testuser", result.getContent().get(0).getUsername());
        assertTrue(result.getContent().get(0).getIsActive());
        assertEquals("inactive_user", result.getContent().get(1).getUsername());
        assertFalse(result.getContent().get(1).getIsActive());

        verify(userRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("getAllUsers - Null pageable üçün IllegalArgumentException verməlidir")
    void getAllUsers_NullPageable_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.getAllUsers(null));
        verifyNoInteractions(userRepository);
    }

    // --- createUser Tests ---

    @Test
    @DisplayName("createUser - Uğurlu yaradılma, parol hash-i və DTO yoxlaması")
    void createUser_Success() {
        UserCreateRequestDto request = new UserCreateRequestDto();
        request.setUsername("  NewUser123  ");
        request.setEmail("  NEWUSER@EXAMPLE.COM  ");
        request.setPassword("strongPassword123!");
        request.setFirstName("  Rəşad  ");
        request.setLastName("  Əliyev  ");
        request.setPhone("  +994509998877  ");
        request.setAddress("  Nizami küç.  ");
        request.setCompanyName("  B2B Auto  ");
        request.setTin("  9998887771  ");

        when(userRepository.findByUsernameIgnoreCase("newuser123")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("newuser@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            u.setCreatedAt(LocalDateTime.now());
            return u;
        });

        UserResponseDto response = userService.createUser(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("newuser123", response.getUsername());
        assertEquals("newuser@example.com", response.getEmail());
        assertEquals("Rəşad", response.getFirstName());
        assertEquals("Əliyev", response.getLastName());
        assertEquals("+994509998877", response.getPhone());
        assertEquals("Nizami küç.", response.getAddress());
        assertEquals("B2B Auto", response.getCompanyName());
        assertEquals("9998887771", response.getTin());
        assertTrue(response.getIsActive());
        assertEquals("USER", response.getRole());
        assertTrue(response.getRoles().contains("USER"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();

        assertTrue(passwordEncoder.matches("strongPassword123!", savedUser.getPassword()),
                "Parol PasswordEncoder.matches ilə doğrulanmalıdır");
        assertTrue(savedUser.getIsActive());
        assertEquals(Role.USER, savedUser.getRole());
    }

    @Test
    @DisplayName("createUser - Null request üçün IllegalArgumentException verməlidir")
    void createUser_NullRequest_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(null));
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("createUser - Boş username və ya email üçün IllegalArgumentException verməlidir")
    void createUser_EmptyUsernameOrEmail_ThrowsException() {
        UserCreateRequestDto emptyUsername = new UserCreateRequestDto();
        emptyUsername.setUsername("   ");
        emptyUsername.setEmail("valid@example.com");
        emptyUsername.setPassword("validSecretPassword15");

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(emptyUsername));

        UserCreateRequestDto emptyEmail = new UserCreateRequestDto();
        emptyEmail.setUsername("validuser");
        emptyEmail.setEmail("   ");
        emptyEmail.setPassword("validSecretPassword15");

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(emptyEmail));
    }

    @Test
    @DisplayName("createUser - Səhv email formatı üçün xəta verməlidir")
    void createUser_InvalidEmailFormat_ThrowsException() {
        UserCreateRequestDto invalidEmail = new UserCreateRequestDto();
        invalidEmail.setUsername("validuser");
        invalidEmail.setEmail("invalid-email-format");
        invalidEmail.setPassword("validSecretPassword15");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(invalidEmail));
        assertTrue(ex.getMessage().contains("formatı"));
    }

    @Test
    @DisplayName("createUser - Təkrar username (böyük-kiçik hərfdən asılı olmayaraq) üçün xəta verməlidir")
    void createUser_DuplicateUsername_ThrowsException() {
        UserCreateRequestDto request = new UserCreateRequestDto();
        request.setUsername("TestUser");
        request.setEmail("different@example.com");
        request.setPassword("validSecretPassword15");

        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(activeUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(request));
        assertTrue(ex.getMessage().contains("istifadəçi adı"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser - Təkrar email (böyük-kiçik hərfdən asılı olmayaraq) üçün xəta verməlidir")
    void createUser_DuplicateEmail_ThrowsException() {
        UserCreateRequestDto request = new UserCreateRequestDto();
        request.setUsername("uniqueuser");
        request.setEmail("ALI@EXAMPLE.COM");
        request.setPassword("validSecretPassword15");

        when(userRepository.findByUsernameIgnoreCase("uniqueuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ali@example.com")).thenReturn(Optional.of(activeUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(request));
        assertTrue(ex.getMessage().contains("e-poçt ünvanı"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser - Parol 12 simvoldan qısa olduqda rədd edilməlidir")
    void createUser_PasswordTooShort_ThrowsException() {
        UserCreateRequestDto request = new UserCreateRequestDto();
        request.setUsername("validuser");
        request.setEmail("valid@example.com");
        request.setPassword("short12345"); // 10 chars (< 12)

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(request));
        assertTrue(ex.getMessage().contains("12 simvol"));
    }

    @Test
    @DisplayName("createUser - Parol dəqiq 12 simvol olduqda uğurla qəbul edilməlidir")
    void createUser_Password12Chars_Success() {
        UserCreateRequestDto request = new UserCreateRequestDto();
        request.setUsername("validuser12");
        request.setEmail("valid12@example.com");
        request.setPassword("exact12chars"); // 12 chars

        when(userRepository.findByUsernameIgnoreCase("validuser12")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("valid12@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(99L);
            return u;
        });

        UserResponseDto response = userService.createUser(request);
        assertNotNull(response);
        assertEquals(99L, response.getId());
        assertEquals("validuser12", response.getUsername());
    }

    @Test
    @DisplayName("createUser - Parol 72 UTF-8 baytdan çox olduqda rədd edilməlidir")
    void createUser_PasswordTooLongInBytes_ThrowsException() {
        // Hər bir 'ş' hərfi UTF-8-də 2 baytdır. 40 dənə 'ş' = 80 bayt (> 72 bayt)
        String longPassword = "ş".repeat(40);
        assertTrue(longPassword.getBytes(StandardCharsets.UTF_8).length > 72);

        UserCreateRequestDto request = new UserCreateRequestDto();
        request.setUsername("validuser");
        request.setEmail("valid@example.com");
        request.setPassword(longPassword);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(request));
        assertTrue(ex.getMessage().contains("72 bayt"));
    }

    @Test
    @DisplayName("createUser - Boş parol rədd edilməlidir")
    void createUser_EmptyPassword_ThrowsException() {
        UserCreateRequestDto request = new UserCreateRequestDto();
        request.setUsername("validuser");
        request.setEmail("valid@example.com");
        request.setPassword("");

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(request));
    }

    // --- updateUser Tests ---

    @Test
    @DisplayName("updateUser - Uğurlu profil redaktəsi və istifadəçinin öz ID-si təkrar yoxlamasından istisna olmalıdır")
    void updateUser_Success_OwnIdIgnored() {
        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setUsername("TESTUSER"); // same username, different casing
        request.setEmail("ALI@EXAMPLE.COM"); // same email, different casing
        request.setFirstName("Əliş");
        request.setLastName("Məmmədli");
        request.setPhone("+994551112233");
        request.setAddress("Nəsimi r.");
        request.setCompanyName("Avto MMC");
        request.setTin("9876543210");

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(activeUser));
        when(userRepository.findByEmailIgnoreCase("ali@example.com")).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String originalPassword = activeUser.getPassword();
        LocalDateTime originalCreatedAt = activeUser.getCreatedAt();

        UserResponseDto response = userService.updateUser(1L, request);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("ali@example.com", response.getEmail());
        assertEquals("Əliş", response.getFirstName());
        assertEquals("Məmmədli", response.getLastName());
        assertEquals("+994551112233", response.getPhone());
        assertEquals("Nəsimi r.", response.getAddress());
        assertEquals("Avto MMC", response.getCompanyName());
        assertEquals("9876543210", response.getTin());

        // Parol, createdAt, isActive toxunulmamalıdır
        assertEquals(originalPassword, activeUser.getPassword());
        assertEquals(originalCreatedAt, activeUser.getCreatedAt());
        assertTrue(activeUser.getIsActive());
        assertEquals(Role.USER, activeUser.getRole());
    }

    @Test
    @DisplayName("updateUser - Başqa istifadəçinin username-i ilə redaktə cəhdi rədd edilməlidir")
    void updateUser_DuplicateUsernameFromOtherUser_ThrowsException() {
        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setUsername("inactive_user");
        request.setEmail("ali@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.findByUsernameIgnoreCase("inactive_user")).thenReturn(Optional.of(inactiveUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser(1L, request));
        assertTrue(ex.getMessage().contains("istifadəçi adı"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser - Başqa istifadəçinin email-i ilə redaktə cəhdi rədd edilməlidir")
    void updateUser_DuplicateEmailFromOtherUser_ThrowsException() {
        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setUsername("testuser");
        request.setEmail("inactive@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(activeUser));
        when(userRepository.findByEmailIgnoreCase("inactive@example.com")).thenReturn(Optional.of(inactiveUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser(1L, request));
        assertTrue(ex.getMessage().contains("e-poçt"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser - Tapılmayan ID üçün EntityNotFoundException verməlidir")
    void updateUser_NotFound_ThrowsEntityNotFoundException() {
        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setUsername("user");
        request.setEmail("user@example.com");

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.updateUser(99L, request));
    }

    // --- setUserActive Tests ---

    @Test
    @DisplayName("setUserActive - İstifadəçini deaktiv və aktiv etmək")
    void setUserActive_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto deactivated = userService.setUserActive(1L, false);
        assertFalse(deactivated.getIsActive());
        assertFalse(activeUser.getIsActive());

        UserResponseDto activated = userService.setUserActive(1L, true);
        assertTrue(activated.getIsActive());
        assertTrue(activeUser.getIsActive());

        verify(userRepository, times(2)).save(activeUser);
    }

    @Test
    @DisplayName("setUserActive - Mənfi ID üçün IllegalArgumentException verməlidir")
    void setUserActive_NegativeId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.setUserActive(-1L, true));
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("setUserActive - Tapılmayan ID üçün EntityNotFoundException verməlidir")
    void setUserActive_NotFound_ThrowsEntityNotFoundException() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> userService.setUserActive(404L, false));
    }
}
