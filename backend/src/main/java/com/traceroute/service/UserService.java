package com.traceroute.service;

import com.traceroute.dto.UserResponse;
import com.traceroute.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository users;
    public UserService(UserRepository users) { this.users = users; }
    public static String normalizeEmail(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    public UserResponse current(String email) {
        return UserResponse.from(users.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials")));
    }
    public List<UserResponse> all() { return users.findAll(org.springframework.data.domain.Sort.by("id")).stream().map(UserResponse::from).toList(); }
}
