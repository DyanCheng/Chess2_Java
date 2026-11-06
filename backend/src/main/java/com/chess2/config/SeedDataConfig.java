package com.chess2.config;

import com.chess2.domain.FunctionCard;
import com.chess2.domain.Puzzle;
import com.chess2.domain.ShopItem;
import com.chess2.repository.FunctionCardRepository;
import com.chess2.repository.PuzzleRepository;
import com.chess2.repository.ShopItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeedDataConfig {
    @Bean CommandLineRunner seedGameContent(FunctionCardRepository cards, ShopItemRepository shop, PuzzleRepository puzzles) {
        return args -> {
            addCard(cards, shop, "HEAL_20", "Healing Spring", "HEAL", 20, 120);
            addCard(cards, shop, "ARCANE_STRIKE", "Arcane Strike", "DOUBLE_DAMAGE", 25, 180);
            addCard(cards, shop, "FREEZE", "Freeze", "FREEZE", 1, 150);
            addCard(cards, shop, "ARSENAL", "Arsenal", "INVULNERABLE", 2, 220);
            addCard(cards, shop, "THAN_TAI", "Thần tài", "EXTRA_QUEEN", 80, 260);
            addCard(cards, shop, "VCKBLA", "VCKBLA", "SURRENDER", 0, 1);
            if (puzzles.count() == 0) puzzles.save(new Puzzle("[[\"bK\",null,null,null,null,null,null,null],[null,null,null,null,null,null,null,null],[null,null,null,null,null,null,null,null],[null,null,null,null,null,null,null,null],[null,null,null,null,null,null,null,null],[null,null,null,null,null,null,null,null],[null,null,null,null,\"wR\",null,null,null],[null,null,null,null,\"wK\",null,null,null]]","e2","e8",75));
        };
    }
    private static void addCard(FunctionCardRepository cards, ShopItemRepository shop, String code, String name, String effect, int value, long price) {
        FunctionCard card = cards.findByCode(code).orElseGet(() -> cards.save(new FunctionCard(code, name, effect, value, price)));
        if (!shop.existsByCardId(card.getId())) shop.save(new ShopItem(name, card.getId(), price));
    }
}
