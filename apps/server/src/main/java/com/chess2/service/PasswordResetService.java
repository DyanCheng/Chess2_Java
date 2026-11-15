package com.chess2.service;

import com.chess2.api.ApiException;
import com.chess2.domain.UserAccount;
import com.chess2.repository.PasswordResetTokenRepository;
import com.chess2.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetService {
    private final UserRepository users; private final PasswordResetTokenRepository tokens; private final PasswordEncoder encoder; private final ObjectProvider<JavaMailSender> mail;
    @Value("${app.password-reset.public-url}") private String publicUrl;
    @Value("${app.password-reset.mail-from:}") private String from;
    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens, PasswordEncoder encoder, ObjectProvider<JavaMailSender> mail) { this.users = users; this.tokens = tokens; this.encoder = encoder; this.mail = mail; }
    @Transactional public void request(String email) {
        JavaMailSender sender = mail.getIfAvailable();
        if (sender == null || from.isBlank()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Chức năng khôi phục mật khẩu chưa được cấu hình email.");
        UserAccount user = users.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (user == null || user.isGuest()) return;
        tokens.deleteByUserId(user.getId());
        String raw = newToken();
        tokens.save(new com.chess2.domain.PasswordResetToken(user.getId(), hash(raw), Instant.now().plus(15, ChronoUnit.MINUTES)));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(user.getEmail()); message.setSubject("Chess2 – đặt lại mật khẩu");
        message.setText("Bạn đã yêu cầu đặt lại mật khẩu Chess2. Link có hiệu lực 15 phút:\n\n" + publicUrl + "?reset=" + raw + "\n\nNếu không phải bạn, hãy bỏ qua email này.");
        sender.send(message);
    }
    @Transactional public void reset(String rawToken, String password) {
        var token = tokens.findByTokenHash(hash(rawToken)).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn."));
        if (!token.isUsable()) throw new ApiException(HttpStatus.BAD_REQUEST, "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
        UserAccount user = users.findById(token.getUserId()).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Liên kết đặt lại mật khẩu không hợp lệ."));
        user.changePassword(encoder.encode(password)); token.use();
    }
    private String newToken() { byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String hash(String value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception error) { throw new IllegalStateException(error); } }
}
