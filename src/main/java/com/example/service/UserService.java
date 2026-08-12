package com.example.service;

import java.util.List;

import com.example.dto.UserRequestDto;
import com.example.dto.UserResponseDto;

public interface UserService {

    UserResponseDto createUser(UserRequestDto userRequestDto);

    List<UserResponseDto> getAllUsers();

    UserResponseDto getUserById(Long id);

    UserResponseDto updateUser(Long id, UserRequestDto userRequestDto);

    String deleteUser(Long id);

}
