package com.zelia.agenda.domain.user.dto;

import com.zelia.agenda.domain.user.User;
import com.zelia.agenda.domain.user.UserType;

public record UserResponse(Long id, String name, String email, UserType userType, Boolean active) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getUserType(), user.getActive());
    }
}