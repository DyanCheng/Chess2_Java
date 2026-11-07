package com.chess2.service;

import com.chess2.api.ApiException;
import com.chess2.domain.UserAccount;
import com.chess2.domain.Wallet;
import com.chess2.dto.AuthDtos;
import com.chess2.repository.UserRepository;
import com.chess2.repository.WalletRepository;
import com.chess2.security.JwtService;
import jakarta.transaction.Transactional;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users; private final WalletRepository wallets; private final PasswordEncoder encoder; private final JwtService jwt;
    public AuthService(UserRepository users, WalletRepository wallets, PasswordEncoder encoder, JwtService jwt) { this.users=users; this.wallets=wallets; this.encoder=encoder; this.jwt=jwt; }
    @Transactional public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.findByUsernameIgnoreCase(username).isPresent()) throw new ApiException(HttpStatus.CONFLICT, "Tên người chơi đã được sử dụng");
        if (users.findByEmailIgnoreCase(email).isPresent()) throw new ApiException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        UserAccount user = users.save(new UserAccount(username, email, encoder.encode(request.password())));
        Wallet wallet = wallets.save(new Wallet(user.getId())); wallet.creditGold(500);
        return response(user, wallet);
    }
    @Transactional public AuthDtos.AuthResponse guest(AuthDtos.GuestRequest request) {
        String guestKey = request.guestKey().toLowerCase(Locale.ROOT);
        UserAccount existing = users.findByGuestKey(guestKey).orElse(null);
        if (existing != null) return response(existing, wallet(existing));
        String suffix = guestKey.replace("-", "").substring(0, 10);
        UserAccount user = UserAccount.guest("guest_" + suffix, "guest+" + guestKey + "@chess2.local", guestKey,
                encoder.encode(UUID.randomUUID().toString() + UUID.randomUUID()));
        user = users.save(user);
        Wallet wallet = wallets.save(new Wallet(user.getId())); wallet.creditGold(250);
        return response(user, wallet);
    }
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        UserAccount user = users.findByUsernameIgnoreCase(request.username()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        if (!encoder.matches(request.password(), user.getPassword())) throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        return response(user, wallet(user));
    }
    public AuthDtos.PlayerResponse profile(UserAccount user) { Wallet wallet=wallet(user); return player(user, wallet); }
    private AuthDtos.AuthResponse response(UserAccount user, Wallet wallet) { return new AuthDtos.AuthResponse(jwt.issue(user.getId().toString(), user.getRole().name()), "Bearer", player(user, wallet)); }
    private AuthDtos.PlayerResponse player(UserAccount user, Wallet wallet) { return new AuthDtos.PlayerResponse(user.getUsername(), user.getEmail(), user.getRole().name(), user.getAccountType().name(), user.getLevel(), user.getExperience(), user.getRating(), user.getWins(), user.getLosses(), user.getDraws(), wallet.getGold(), wallet.getGems(), user.getId()); }
    private Wallet wallet(UserAccount user) { return wallets.findById(user.getId()).orElseGet(() -> wallets.save(new Wallet(user.getId()))); }
}
