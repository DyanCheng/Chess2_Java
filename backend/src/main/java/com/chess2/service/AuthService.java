package com.chess2.service;

import com.chess2.api.ApiException;
import com.chess2.domain.UserAccount;
import com.chess2.domain.Wallet;
import com.chess2.dto.AuthDtos;
import com.chess2.repository.UserRepository;
import com.chess2.repository.WalletRepository;
import com.chess2.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users; private final WalletRepository wallets; private final PasswordEncoder encoder; private final JwtService jwt;
    public AuthService(UserRepository users, WalletRepository wallets, PasswordEncoder encoder, JwtService jwt) { this.users=users; this.wallets=wallets; this.encoder=encoder; this.jwt=jwt; }
    @Transactional public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (users.findByUsernameIgnoreCase(request.username()).isPresent()) throw new ApiException(HttpStatus.CONFLICT, "Username is already in use");
        if (users.findByEmailIgnoreCase(request.email()).isPresent()) throw new ApiException(HttpStatus.CONFLICT, "Email is already in use");
        UserAccount user = users.save(new UserAccount(request.username().trim(), request.email().trim().toLowerCase(), encoder.encode(request.password())));
        Wallet wallet = wallets.save(new Wallet(user.getId())); wallet.creditGold(500);
        return response(user, wallet);
    }
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        UserAccount user = users.findByUsernameIgnoreCase(request.username()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        if (!encoder.matches(request.password(), user.getPassword())) throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        return response(user, wallet(user));
    }
    public AuthDtos.PlayerResponse profile(UserAccount user) { Wallet wallet=wallet(user); return new AuthDtos.PlayerResponse(user.getUsername(), user.getEmail(), user.getRole().name(), user.getLevel(), user.getExperience(), wallet.getGold(), wallet.getGems()); }
    private AuthDtos.AuthResponse response(UserAccount user, Wallet wallet) { return new AuthDtos.AuthResponse(jwt.issue(user.getUsername(), user.getRole().name()), "Bearer", new AuthDtos.PlayerResponse(user.getUsername(), user.getEmail(), user.getRole().name(), user.getLevel(), user.getExperience(), wallet.getGold(), wallet.getGems())); }
    private Wallet wallet(UserAccount user) { return wallets.findById(user.getId()).orElseGet(() -> wallets.save(new Wallet(user.getId()))); }
}
