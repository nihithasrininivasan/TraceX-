package com.traceroute.controller;

import com.traceroute.dto.UserResponse;
import com.traceroute.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService users;
    public UserController(UserService users) { this.users = users; }
    @GetMapping("/me")
    public UserResponse me(Principal principal) { return users.current(principal.getName()); }
    @GetMapping
    public List<UserResponse> all() { return users.all(); }
}
