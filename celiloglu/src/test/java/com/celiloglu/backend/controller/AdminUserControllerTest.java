package com.celiloglu.backend.controller;

import com.celiloglu.backend.dto.request.UserCreateRequestDto;
import com.celiloglu.backend.dto.request.UserUpdateRequestDto;
import com.celiloglu.backend.dto.response.UserResponseDto;
import com.celiloglu.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminUserController adminUserController;

    private UserResponseDto testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController).build();

        testUser = new UserResponseDto();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Əli");
        testUser.setLastName("Məmmədov");
        testUser.setRole("USER");
        testUser.setIsActive(true);
    }

    @Test
    @DisplayName("GET /admin/users - Siyahı və yaratma forması uğurla göstərilir")
    void testListUsers_Success() throws Exception {
        Page<UserResponseDto> page = new PageImpl<>(List.of(testUser));
        when(userService.getAllUsers(any())).thenReturn(page);

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users", "userPage", "createForm", "updateForm", "isEditMode"))
                .andExpect(model().attribute("isEditMode", false));

        verify(userService, times(1)).getAllUsers(any());
    }

    @Test
    @DisplayName("GET /admin/users/{id}/edit - Redaktə rejimi uğurla açılır")
    void testEditUser_Success() throws Exception {
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(userService.getAllUsers(any())).thenReturn(new PageImpl<>(List.of(testUser)));

        mockMvc.perform(get("/admin/users/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attribute("isEditMode", true))
                .andExpect(model().attribute("editingUserId", 1L))
                .andExpect(model().attributeExists("updateForm"));

        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    @DisplayName("GET /admin/users/{id}/edit - Tapılmayan ID üçün redirect və flash error verilir")
    void testEditUser_NotFound() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new EntityNotFoundException("İstifadəçi tapılmadı! ID: 99"));

        mockMvc.perform(get("/admin/users/99/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("userError"));
    }

    @Test
    @DisplayName("POST /admin/users - Uğurlu yaradılma zamanı redirect və flash success verilir")
    void testCreateUser_Success() throws Exception {
        when(userService.createUser(any(UserCreateRequestDto.class))).thenReturn(testUser);

        mockMvc.perform(post("/admin/users")
                        .param("username", "newuser")
                        .param("email", "newuser@example.com")
                        .param("password", "strongPassword12345"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("userSuccess"));

        verify(userService, times(1)).createUser(any(UserCreateRequestDto.class));
    }

    @Test
    @DisplayName("POST /admin/users - Xəta zamanı eyni səhifə açılır və xəta mesajı göstərilir")
    void testCreateUser_ValidationError() throws Exception {
        when(userService.createUser(any(UserCreateRequestDto.class)))
                .thenThrow(new IllegalArgumentException("Şifrə ən azı 12 simvoldan ibarət olmalıdır!"));
        when(userService.getAllUsers(any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(post("/admin/users")
                        .param("username", "newuser")
                        .param("email", "newuser@example.com")
                        .param("password", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("userError"));
    }

    @Test
    @DisplayName("POST /admin/users/{id} - Uğurlu redaktə zamanı redirect və flash success verilir")
    void testUpdateUser_Success() throws Exception {
        when(userService.updateUser(eq(1L), any(UserUpdateRequestDto.class))).thenReturn(testUser);

        mockMvc.perform(post("/admin/users/1")
                        .param("username", "updateduser")
                        .param("email", "updated@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("userSuccess"));

        verify(userService, times(1)).updateUser(eq(1L), any(UserUpdateRequestDto.class));
    }

    @Test
    @DisplayName("POST /admin/users/{id} - Redaktə xətası zamanı eyni səhifə və edit rejimi qalır")
    void testUpdateUser_ValidationError() throws Exception {
        when(userService.updateUser(eq(1L), any(UserUpdateRequestDto.class)))
                .thenThrow(new IllegalArgumentException("Bu istifadəçi adı artıq istifadə olunur!"));
        when(userService.getAllUsers(any())).thenReturn(new PageImpl<>(List.of(testUser)));

        mockMvc.perform(post("/admin/users/1")
                        .param("username", "takenuser")
                        .param("email", "updated@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("userError"))
                .andExpect(model().attribute("isEditMode", true));
    }

    @Test
    @DisplayName("POST /admin/users/{id}/activate - İstifadəçi aktivləşdirilir")
    void testActivateUser_Success() throws Exception {
        when(userService.setUserActive(1L, true)).thenReturn(testUser);

        mockMvc.perform(post("/admin/users/1/activate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("userSuccess"));

        verify(userService, times(1)).setUserActive(1L, true);
    }

    @Test
    @DisplayName("POST /admin/users/{id}/deactivate - İstifadəçi deaktiv edilir")
    void testDeactivateUser_Success() throws Exception {
        when(userService.setUserActive(1L, false)).thenReturn(testUser);

        mockMvc.perform(post("/admin/users/1/deactivate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("userSuccess"));

        verify(userService, times(1)).setUserActive(1L, false);
    }

    @Test
    @DisplayName("GET /admin/users - Thymeleaf şablonunun (admin/users.html) xətasız tam render olunması")
    void testRenderTemplate_Success() throws Exception {
        org.thymeleaf.templateresolver.ClassLoaderTemplateResolver templateResolver =
                new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine);
        viewResolver.setCharacterEncoding("UTF-8");

        MockMvc mvcWithThymeleaf = MockMvcBuilders.standaloneSetup(adminUserController)
                .setControllerAdvice(new SecurityModelAdvice(mock(com.celiloglu.backend.service.CartService.class)))
                .setViewResolvers(viewResolver)
                .build();

        when(userService.getAllUsers(any())).thenReturn(new PageImpl<>(List.of(testUser)));

        mvcWithThymeleaf.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cəliloğlu - İstifadəçilər")))
                .andExpect(content().string(containsString("Yeni istifadəçi yarat")))
                .andExpect(content().string(containsString("testuser")));
    }
}
