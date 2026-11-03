package com.traceroute.dto;

import com.traceroute.entity.Role;
import com.traceroute.entity.User;

public record UserResponse(Long id, String name, String email, Role role) {
    public static UserResponse from(User user) { return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole()); }
}
