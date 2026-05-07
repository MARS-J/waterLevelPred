package com.waterlevel.server.service.impl;

import com.waterlevel.server.dto.LoginRequest;
import com.waterlevel.server.dto.LoginResponse;
import com.waterlevel.server.entity.User;
import com.waterlevel.server.repository.UserRepository;
import com.waterlevel.server.service.AuthService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    public AuthServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        User user = userOpt.get();

        if (!user.getEnabled()) {
            throw new IllegalArgumentException("账号已被禁用");
        }

        String hashedPassword = sha256(request.getPassword());
        if (!hashedPassword.equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole()
        );
    }

    static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
