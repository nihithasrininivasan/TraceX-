package com.traceroute.service;

import com.traceroute.dto.*;
import com.traceroute.entity.*;
import com.traceroute.exception.DuplicateEmailException;
import com.traceroute.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    public AuthService(UserRepository users, PasswordEncoder encoder, AuthenticationManager authenticationManager, UserService userService) {
        this.users = users; this.encoder = encoder; this.authenticationManager = authenticationManager; this.userService = userService;
    }
    public UserResponse register(RegisterRequest request) {
        String email = UserService.normalizeEmail(request.email());
        if (users.existsByEmail(email)) throw new DuplicateEmailException();
        User user = new User(request.name().strip(), email, encoder.encode(request.password()), Role.STUDENT);
        try { return UserResponse.from(users.saveAndFlush(user)); }
        catch (DataIntegrityViolationException exception) {
            if (users.existsByEmail(email)) throw new DuplicateEmailException();
            throw exception;
        }
    }
    public UserResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(UserService.normalizeEmail(request.email()), request.password()));
        return userService.current(authentication.getName());
    }
}
