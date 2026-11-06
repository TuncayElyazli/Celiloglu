package com.celiloglu.backend.security;

import com.celiloglu.backend.config.PasswordConfig;
import com.celiloglu.backend.config.SecurityConfig;
import com.celiloglu.backend.controller.CartController;
import com.celiloglu.backend.controller.UserController;
import com.celiloglu.backend.entity.User;
import com.celiloglu.backend.enums.Role;
import com.celiloglu.backend.repository.UserRepository;
import com.celiloglu.backend.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        SecurityConfig.class,
        PasswordConfig.class,
        CustomUserDetailsService.class,
        SecurityIntegrationTest.TestWebMvcConfig.class
})
@WebAppConfiguration
class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    private User activeUser;
    private User adminUser;
    private User superAdminUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        reset(userRepository);

        String encodedPassword = passwordEncoder.encode("SecretPassword123!");

        activeUser = new User();
        activeUser.setId(10L);
        activeUser.setUsername("tuncay");
        activeUser.setEmail("tuncay@example.com");
        activeUser.setPassword(encodedPassword);
        activeUser.setIsActive(true);
        activeUser.setRole(Role.USER);
        activeUser.setRoles(new HashSet<>(Set.of(Role.USER)));

        adminUser = new User();
        adminUser.setId(20L);
        adminUser.setUsername("adminuser");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(encodedPassword);
        adminUser.setIsActive(true);
        adminUser.setRole(Role.ADMIN);
        adminUser.setRoles(new HashSet<>(Set.of(Role.ADMIN)));

        superAdminUser = new User();
        superAdminUser.setId(30L);
        superAdminUser.setUsername("superadmin");
        superAdminUser.setEmail("superadmin@example.com");
        superAdminUser.setPassword(encodedPassword);
        superAdminUser.setIsActive(true);
        superAdminUser.setRole(Role.SUPER_ADMIN);
        superAdminUser.setRoles(new HashSet<>(Set.of(Role.SUPER_ADMIN)));

        inactiveUser = new User();
        inactiveUser.setId(40L);
        inactiveUser.setUsername("blockeduser");
        inactiveUser.setEmail("blocked@example.com");
        inactiveUser.setPassword(encodedPassword);
        inactiveUser.setIsActive(false);
        inactiveUser.setRole(Role.USER);
    }

    @Test
    @DisplayName("Username ilə uğurlu giriş və sessiya yaranması")
    void testLoginSuccessWithUsername() throws Exception {
        when(userRepository.findByUsernameIgnoreCase("tuncay")).thenReturn(Optional.of(activeUser));
        when(userRepository.findByEmailIgnoreCase("tuncay")).thenReturn(Optional.empty());

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "tuncay")
                        .param("password", "SecretPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("tuncay").withRoles("USER"));
    }

    @Test
    @DisplayName("Email ilə uğurlu giriş")
    void testLoginSuccessWithEmail() throws Exception {
        when(userRepository.findByUsernameIgnoreCase("tuncay@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("tuncay@example.com")).thenReturn(Optional.of(activeUser));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "tuncay@example.com")
                        .param("password", "SecretPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("tuncay"));
    }

    @Test
    @DisplayName("Səhv parol ilə giriş uğursuz olur və /login?error yönləndirir")
    void testLoginWithWrongPassword_shouldFail() throws Exception {
        when(userRepository.findByUsernameIgnoreCase("tuncay")).thenReturn(Optional.of(activeUser));
        when(userRepository.findByEmailIgnoreCase("tuncay")).thenReturn(Optional.empty());

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "tuncay")
                        .param("password", "WrongPassword!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("Deaktiv istifadəçi hesabı ilə giriş uğursuz olmalıdır")
    void testLoginWithDeactivatedUser_shouldFail() throws Exception {
        when(userRepository.findByUsernameIgnoreCase("blockeduser")).thenReturn(Optional.of(inactiveUser));
        when(userRepository.findByEmailIgnoreCase("blockeduser")).thenReturn(Optional.empty());

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "blockeduser")
                        .param("password", "SecretPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("Ziddiyyətli identifikator (username fərqli, email fərqli istifadəçi tapdıqda) rədd edilməlidir")
    void testLoginWithConflictingIdentifier_shouldFail() throws Exception {
        User otherUser = new User();
        otherUser.setId(999L);
        otherUser.setUsername("another");
        otherUser.setEmail("conflict");
        otherUser.setPassword(passwordEncoder.encode("SecretPassword123!"));
        otherUser.setIsActive(true);

        when(userRepository.findByUsernameIgnoreCase("conflict")).thenReturn(Optional.of(activeUser)); // ID 10
        when(userRepository.findByEmailIgnoreCase("conflict")).thenReturn(Optional.of(otherUser));   // ID 999

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "conflict")
                        .param("password", "SecretPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("Ziddiyyətli role/roles zamanı authorities yalnız User.role-dan təyin olunur")
    void testAuthoritiesDeterminedOnlyFromUserRole() throws Exception {
        // activeUser role=USER, roles={ADMIN}
        activeUser.setRole(Role.USER);
        activeUser.setRoles(new HashSet<>(Set.of(Role.ADMIN)));

        when(userRepository.findByUsernameIgnoreCase("tuncay")).thenReturn(Optional.of(activeUser));
        when(userRepository.findByEmailIgnoreCase("tuncay")).thenReturn(Optional.empty());

        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "tuncay")
                        .param("password", "SecretPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated().withRoles("USER"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        org.springframework.security.core.context.SecurityContext secContext =
                (org.springframework.security.core.context.SecurityContext) session.getAttribute(
                        org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(secContext);
        var authorities = secContext.getAuthentication().getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .toList();
        assertTrue(authorities.contains("ROLE_USER"));
        assertFalse(authorities.contains("ROLE_ADMIN"));
    }

    @Test
    @WithMockUser(username = "regularUser", roles = {"USER"})
    @DisplayName("Adi USER rolu olan istifadəçi üçün /admin 403 Forbidden qaytarmalıdır")
    void testAdminAccessWithUserRole_returns403() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin.html"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("ADMIN rolu olan istifadəçi /admin səhifələrinə daxil ola bilər")
    void testAdminAccessWithAdminRole_returns200() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string("admin-dashboard-ok"));

        mockMvc.perform(get("/admin.html"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "superadmin", roles = {"SUPER_ADMIN"})
    @DisplayName("SUPER_ADMIN rolu olan istifadəçi /admin səhifələrinə daxil ola bilər")
    void testAdminAccessWithSuperAdminRole_returns200() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string("admin-dashboard-ok"));
    }

    @Test
    @DisplayName("Anonim istifadəçi qorunan səhifələrə müraciət edərkən /login səhifəsinə yönləndirilməlidir")
    void testAnonymousAccessToProtectedRoutes_redirectsToLogin() throws Exception {
        // Admin
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // Profil
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // Səbət
        mockMvc.perform(get("/cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // Sifarişlər
        mockMvc.perform(get("/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("İctimai səhifələr anonim istifadəçilər üçün açıqdır")
    void testAnonymousAccessToPublicRoutes_isAllowed() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/login.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/about"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/products/100"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CSRF olmadan göndərilən POST sorğusu 403 Forbidden ilə rədd edilməlidir")
    void testPostWithoutCsrf_returns403() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "tuncay")
                        .param("password", "SecretPassword123!"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /logout çıxışı sessiyanı ləğv etməli və /login?logout yönləndirməlidir")
    void testLogout_shouldInvalidateSessionAndRedirect() throws Exception {
        when(userRepository.findByUsernameIgnoreCase("tuncay")).thenReturn(Optional.of(activeUser));
        when(userRepository.findByEmailIgnoreCase("tuncay")).thenReturn(Optional.empty());

        // Əvvəl daxil oluruq və sessiya əldə edirik
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "tuncay")
                        .param("password", "SecretPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session, "Uğurlu girişdən sonra sessiya mövcud olmalıdır");

        // Çıxış edirik
        mockMvc.perform(post("/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        assertTrue(session.isInvalid(), "Çıxış zamanı sessiya ləğv edilməlidir");
    }

    @Test
    @DisplayName("Anonim xəta sorğusu (/error) login tələb etməməlidir və açıq olmalıdır")
    void testAnonymousAccessToError_isAllowedWithoutLoginRedirect() throws Exception {
        mockMvc.perform(get("/error"))
                .andExpect(status().isOk())
                .andExpect(content().string("error-ok"));
    }

    @Test
    @DisplayName("Anonim istifadəçi /cart-a daxil olarkən sessiyada /cart saxlanılır və login sonrası /cart-a yönləndirilir")
    void testLoginRedirectsToSavedCartUrl() throws Exception {
        when(userRepository.findByUsernameIgnoreCase("tuncay")).thenReturn(Optional.of(activeUser));
        when(userRepository.findByEmailIgnoreCase("tuncay")).thenReturn(Optional.empty());

        // 1. Anonim istifadəçi /cart açmağa cəhd edir
        MvcResult cartResult = mockMvc.perform(get("/cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) cartResult.getRequest().getSession(false);
        assertNotNull(session, "Sessiya yaranmalı və qorunan səhifə yadda saxlanmalıdır");

        // 2. İstifadəçi login edir
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .session(session)
                        .param("username", "tuncay")
                        .param("password", "SecretPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("http://localhost/cart*"));
    }

    @Test
    @DisplayName("/error sorğusu RequestCache-ə saxlanılmır və login-dən sonra /error?continue ünvanına yönləndirmir")
    void testErrorNotSavedInRequestCache() throws Exception {
        when(userRepository.findByUsernameIgnoreCase("tuncay")).thenReturn(Optional.of(activeUser));
        when(userRepository.findByEmailIgnoreCase("tuncay")).thenReturn(Optional.empty());

        // 1. Anonim istifadəçi /error sorğusu göndərir
        MvcResult errorResult = mockMvc.perform(get("/error"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) errorResult.getRequest().getSession(false);

        // 2. İstifadəçi login edir (hər hansı yadda qalan /error olmamalıdır, default '/' ünvanına getməlidir)
        var postRequest = post("/login")
                .with(csrf())
                .param("username", "tuncay")
                .param("password", "SecretPassword123!");
        if (session != null) {
            postRequest.session(session);
        }

        mockMvc.perform(postRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Configuration
    @EnableWebMvc
    static class TestWebMvcConfig {

        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        public UserController userController() {
            return new UserController();
        }

        @Bean
        public CartService cartService() {
            return mock(CartService.class);
        }

        @Bean
        public CartController cartController(CartService cartService) {
            return new CartController(cartService);
        }

        @Bean
        public org.springframework.web.servlet.ViewResolver viewResolver() {
            org.springframework.web.servlet.view.InternalResourceViewResolver resolver =
                    new org.springframework.web.servlet.view.InternalResourceViewResolver();
            resolver.setPrefix("/templates/");
            resolver.setSuffix(".html");
            return resolver;
        }

        @Bean
        public TestPublicAndAdminController testPublicAndAdminController() {
            return new TestPublicAndAdminController();
        }
    }

    @Controller
    static class TestPublicAndAdminController {

        @GetMapping({"/", "/index", "/index.html"})
        @ResponseBody
        public String home() {
            return "home-ok";
        }

        @GetMapping({"/about", "/about.html"})
        @ResponseBody
        public String about() {
            return "about-ok";
        }

        @GetMapping("/products/{id}")
        @ResponseBody
        public String productDetails() {
            return "product-details-ok";
        }

        @GetMapping({"/admin", "/admin.html", "/admin/users"})
        @ResponseBody
        public String adminDashboard() {
            return "admin-dashboard-ok";
        }

        @GetMapping("/error")
        @ResponseBody
        public String error() {
            return "error-ok";
        }
    }
}
