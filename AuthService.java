package com.falafacil.service;

import com.falafacil.dto.LoginRequest;
import com.falafacil.dto.LoginResponse;
import com.falafacil.dto.RegisterRequest;
import com.falafacil.entity.User;
import com.falafacil.exception.CustomException;
import com.falafacil.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException("Email já cadastrado", "DUPLICATE_EMAIL", 400);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setActive(true);

        userRepository.save(user);
        log.info("Novo usuário registrado: {}", request.getEmail());

        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900) // 15 minutos em segundos
                .user(com.falafacil.dto.UserResponse.fromEntity(user))
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException("Credenciais inválidas", "INVALID_CREDENTIALS", 401));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException("Credenciais inválidas", "INVALID_CREDENTIALS", 401);
        }

        if (!user.isActive()) {
            throw new CustomException("Usuário inativo", "USER_INACTIVE", 403);
        }

        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        log.info("Login bem-sucedido para: {}", request.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900)
                .user(com.falafacil.dto.UserResponse.fromEntity(user))
                .build();
    }

    public LoginResponse refreshToken(String token) {
        String tokenValue = token.replace("Bearer ", "");

        if (!jwtTokenProvider.validateToken(tokenValue)) {
            throw new CustomException("Token inválido", "INVALID_TOKEN", 401);
        }

        if (!jwtTokenProvider.isRefreshToken(tokenValue)) {
            throw new CustomException("Token não é um refresh token", "INVALID_TOKEN_TYPE", 401);
        }

        String email = jwtTokenProvider.getUsernameFromToken(tokenValue);
        String newAccessToken = jwtTokenProvider.generateToken(email);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(tokenValue)
                .expiresIn(900)
                .build();
    }

    public void logout(String token) {
        String tokenValue = token.replace("Bearer ", "");
        tokenBlacklistService.addToBlacklist(tokenValue);
        log.info("Logout realizado");
    }
}
