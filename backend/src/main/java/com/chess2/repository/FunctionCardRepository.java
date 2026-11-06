package com.chess2.repository;

import com.chess2.domain.FunctionCard;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FunctionCardRepository extends JpaRepository<FunctionCard, UUID> { Optional<FunctionCard> findByCode(String code); }
