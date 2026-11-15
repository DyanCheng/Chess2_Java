package com.chess2.config;

import com.chess2.domain.UserAccount;
import com.chess2.domain.Wallet;
import com.chess2.repository.UserRepository;
import com.chess2.repository.WalletRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrapConfig {
    @Bean CommandLineRunner bootstrapAdmin(UserRepository users,WalletRepository wallets,PasswordEncoder encoder,@Value("${app.admin.username:}") String username,@Value("${app.admin.email:}") String email,@Value("${app.admin.password:}") String password){
        return args->{if(username.isBlank()||email.isBlank()||password.length()<12)return;UserAccount admin=users.findByUsernameIgnoreCase(username.trim()).orElseGet(()->users.save(new UserAccount(username.trim(),email.trim().toLowerCase(Locale.ROOT),encoder.encode(password))));admin.grantAdmin();admin=users.save(admin);if(!wallets.existsById(admin.getId()))wallets.save(new Wallet(admin.getId()));};
    }
}
