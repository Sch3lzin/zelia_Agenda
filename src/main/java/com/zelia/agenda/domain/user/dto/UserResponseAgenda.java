package com.zelia.agenda.domain.user.dto;

import com.zelia.agenda.domain.user.User;

public record UserResponseAgenda(String name) {
    public static UserResponseAgenda from(User user) {
        return new UserResponseAgenda(user.getName());
    }
}
