package com.celiloglu.backend.security;

import com.celiloglu.backend.entity.User;
import com.celiloglu.backend.enums.Role;
import com.celiloglu.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setUsername("testuser");
        activeUser.setEmail("test@example.com");
        activeUser.setPassword("$2a$10$hashedPasswordExample");
        activeUser.setIsActive(true);
        activeUser.setRole(Role.USER);
        activeUser.setRoles(new HashSet<>(Set.of(Role.ADMIN))); // roles ziddiyyətli olsa belə yalnız role götürülməlidir

        inactiveUser = new User();
        inactiveUser.setId(2L);
        inactiveUser.setUsername("deactiveuser");
        inactiveUser.setEmail("deactive@example.com");
        inactiveUser.setPassword("$2a$10$hashedPasswordExample");
        inactiveUser.setIsActive(false);
        inactiveUser.setRole(Role.USER);
    }

    @Test
    @DisplayName("Username ilə uğurlu istifadəçi tapılması")
    void loadUserByUsername_whenUsernameMatches_shouldReturnUserDetails() {
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(activeUser));
        when(userRepository.findByEmailIgnoreCase("testuser")).thenReturn(Optional.empty());

        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("$2a$10$hashedPasswordExample", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails instanceof CustomUserDetails);
        assertEquals(1L, ((CustomUserDetails) userDetails).getId());

        // Authorities yalnız User.role-dan qurulmalıdır (ROLE_USER)
        assertEquals(1, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_USER")));
        assertFalse(userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN")), "roles sahəsindən olan dəyər daxil edilməməlidir");
    }

    @Test
    @DisplayName("Email ilə uğurlu istifadəçi tapılması")
    void loadUserByUsername_whenEmailMatches_shouldReturnUserDetails() {
        when(userRepository.findByUsernameIgnoreCase("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(activeUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertTrue(userDetails.isEnabled());
    }

    @Test
    @DisplayName("Giriş identifikatoru trim edilməlidir")
    void loadUserByUsername_shouldTrimIdentifier() {
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(activeUser));
        when(userRepository.findByEmailIgnoreCase("testuser")).thenReturn(Optional.empty());

        UserDetails userDetails = userDetailsService.loadUserByUsername("   testuser   ");

        assertNotNull(userDetails);
        verify(userRepository).findByUsernameIgnoreCase("testuser");
        verify(userRepository).findByEmailIgnoreCase("testuser");
    }

    @Test
    @DisplayName("Hər iki axtarış fərqli istifadəçiləri qaytararsa giriş rədd edilməlidir")
    void loadUserByUsername_whenConflictingUsersFound_shouldThrowException() {
        User user1 = new User();
        user1.setId(10L);
        user1.setUsername("duplicate");
        user1.setEmail("user10@example.com");

        User user2 = new User();
        user2.setId(20L);
        user2.setUsername("other");
        user2.setEmail("duplicate");

        when(userRepository.findByUsernameIgnoreCase("duplicate")).thenReturn(Optional.of(user1));
        when(userRepository.findByEmailIgnoreCase("duplicate")).thenReturn(Optional.of(user2));

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("duplicate")
        );

        assertTrue(ex.getMessage().contains("ziddiyyətli"));
    }

    @Test
    @DisplayName("Hər iki axtarış eyni istifadəçini qaytararsa uğurla qəbul edilməlidir")
    void loadUserByUsername_whenBothFindSameUser_shouldSucceed() {
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(activeUser));
        when(userRepository.findByEmailIgnoreCase("testuser")).thenReturn(Optional.of(activeUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
    }

    @Test
    @DisplayName("Deaktiv istifadəçi üçün isEnabled false olmalıdır")
    void loadUserByUsername_whenUserIsInactive_shouldSetEnabledFalse() {
        when(userRepository.findByUsernameIgnoreCase("deactiveuser")).thenReturn(Optional.of(inactiveUser));
        when(userRepository.findByEmailIgnoreCase("deactiveuser")).thenReturn(Optional.empty());

        UserDetails userDetails = userDetailsService.loadUserByUsername("deactiveuser");

        assertNotNull(userDetails);
        assertFalse(userDetails.isEnabled());
    }

    @Test
    @DisplayName("Tapılmayan istifadəçi üçün UsernameNotFoundException atılmalıdır")
    void loadUserByUsername_whenUserNotFound_shouldThrowException() {
        when(userRepository.findByUsernameIgnoreCase("notfound")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("notfound")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("notfound"));
    }

    @Test
    @DisplayName("Boş və ya null identifikator qəbul edilməməlidir")
    void loadUserByUsername_whenEmptyOrNull_shouldThrowException() {
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(null));
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(""));
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("    "));
    }

    @Test
    @DisplayName("CustomUserDetails toString metodunda şifrə və ya hash göstərilməməlidir")
    void customUserDetails_toString_shouldNotContainPassword() {
        CustomUserDetails details = new CustomUserDetails(1L, "tuncay", "secret_hash_value", true, Set.of());
        String str = details.toString();

        assertFalse(str.contains("secret_hash_value"), "toString şifrə və ya hash saxlamamalıdır!");
        assertTrue(str.contains("tuncay"));
        assertTrue(str.contains("id=1"));
    }
}
