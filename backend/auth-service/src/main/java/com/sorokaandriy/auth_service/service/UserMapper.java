package com.sorokaandriy.auth_service.service;

import com.sorokaandriy.auth_service.dto.RegisterRequest;
import com.sorokaandriy.auth_service.dto.UserResponse;
import com.sorokaandriy.auth_service.entity.Role;
import com.sorokaandriy.auth_service.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    public User fromRegisterRequestToUser(RegisterRequest request, String encodedPassword){

        return User.builder()
                .email(request.email().toLowerCase())
                .password(encodedPassword)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .role(Role.USER)
                .build();
    }

    public UserResponse fromUserToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId().toString())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
