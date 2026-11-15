package com.chess2.repository;

import com.chess2.domain.ShopItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopItemRepository extends JpaRepository<ShopItem, UUID> { List<ShopItem> findByActiveTrue(); boolean existsByCardId(UUID cardId); }
