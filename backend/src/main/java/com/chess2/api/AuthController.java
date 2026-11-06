package com.chess2.api;

import com.chess2.domain.UserAccount;
import com.chess2.dto.AuthDtos;
import com.chess2.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth=auth; }
    @PostMapping("/register") public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) { return auth.register(request); }
    @PostMapping("/login") public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) { return auth.login(request); }
    @PostMapping("/guest") public AuthDtos.AuthResponse guest(@Valid @RequestBody AuthDtos.GuestRequest request) { return auth.guest(request); }
    @GetMapping("/me") public AuthDtos.PlayerResponse me(@AuthenticationPrincipal UserAccount user) { return auth.profile(user); }
}
