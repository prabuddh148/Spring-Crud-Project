package com.example.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.dto.UserRequestDto;
import com.example.dto.UserResponseDto;
import com.example.model.Role;
import com.example.model.User;
import com.example.repository.RoleRepository;
import com.example.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDto createUser(UserRequestDto userRequestDto) {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("USER role not found"));

        User user = new User();
        user.setName(userRequestDto.name());
        user.setEmail(userRequestDto.email());
        // encode like AuthService does, otherwise these users can never log in
        user.setPassword(passwordEncoder.encode(userRequestDto.password()));
        user.setRole(userRole);

        User savedUser = userRepository.save(user);

        return new UserResponseDto(savedUser.getId(), savedUser.getName(), savedUser.getEmail());
    }

    @Override
    public UserResponseDto updateUser(Long id, UserRequestDto userRequestDto) {
        User existingUser = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        existingUser.setName(userRequestDto.name());
        existingUser.setEmail(userRequestDto.email());
        existingUser.setPassword(passwordEncoder.encode(userRequestDto.password()));

        User savedUser = userRepository.save(existingUser);

        return new UserResponseDto(savedUser.getId(), savedUser.getName(), savedUser.getEmail());
    }

    @Override
    public String deleteUser(Long id) {
        userRepository.deleteById(id);

        return "User deleted successfully";

    }

    @Override
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> new UserResponseDto(user.getId(), user.getName(), user.getEmail()))
                .toList();
    }

    @Override
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        return new UserResponseDto(user.getId(), user.getName(), user.getEmail());

    }
}
