package com.celiloglu.backend.service;

import com.celiloglu.backend.dto.request.UserCreateRequestDto;
import com.celiloglu.backend.dto.request.UserUpdateRequestDto;
import com.celiloglu.backend.dto.response.UserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    Page<UserResponseDto> getAllUsers(Pageable pageable);

    UserResponseDto getUserById(Long id);

    UserResponseDto createUser(UserCreateRequestDto request);

    UserResponseDto updateUser(Long id, UserUpdateRequestDto request);

    UserResponseDto setUserActive(Long id, boolean active);
}
