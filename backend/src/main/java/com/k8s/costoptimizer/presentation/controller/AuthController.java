package com.k8s.costoptimizer.presentation.controller;

import com.k8s.costoptimizer.application.dto.AuthRequest;
import com.k8s.costoptimizer.application.dto.AuthResponse;
import com.k8s.costoptimizer.application.dto.RegisterRequest;
import com.k8s.costoptimizer.application.service.AuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller managing credentials-based authentication and token generation.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for login and user management")
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/login")
    @Operation(summary = "Validate user credentials and return standard Bearer JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authUseCase.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Create a new developer dashboard account")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        authUseCase.register(request);
        return ResponseEntity.ok("User registered successfully");
    }
}
