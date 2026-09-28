package com.k8s.costoptimizer.application.service;

import com.k8s.costoptimizer.application.dto.AuthRequest;
import com.k8s.costoptimizer.application.dto.AuthResponse;
import com.k8s.costoptimizer.application.dto.RegisterRequest;
import com.k8s.costoptimizer.infrastructure.persistence.entity.UserEntity;
import com.k8s.costoptimizer.infrastructure.persistence.repository.SpringDataUserRepository;
import com.k8s.costoptimizer.infrastructure.security.JwtTokenProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service that handles user login transactions and account registrations.
 */
@Service
@RequiredArgsConstructor
public class AuthUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthUseCase.class);

    private final AuthenticationManager authenticationManager;
    private final SpringDataUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @PostConstruct
    public void initDefaultUser() {
        if (userRepository.count() == 0) {
            log.info("No user accounts found. Bootstrapping default developer account: admin / admin123");
            UserEntity devAdmin = UserEntity.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@costoptimizer.k8s")
                    .role("ROLE_ADMIN")
                    .build();
            userRepository.save(devAdmin);
        }
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String jwt = tokenProvider.generateToken(authentication);
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User record missing post authentication"));

        return AuthResponse.builder()
                .token(jwt)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role("ROLE_ADMIN") // All registered users are admins in this single-tenant dashboard
                .build();

        userRepository.save(user);
    }
}
