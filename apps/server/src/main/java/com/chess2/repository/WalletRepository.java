package com.chess2.repository;

import com.chess2.domain.Wallet;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, UUID> { }
