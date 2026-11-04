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
            if (cards.count() == 0) {
                FunctionCard heal=cards.save(new FunctionCard("HEAL_20","Healing Spring","HEAL",20,120));
                FunctionCard strike=cards.save(new FunctionCard("ARCANE_STRIKE","Arcane Strike","DOUBLE_DAMAGE",25,180));
                shop.save(new ShopItem("Healing Spring",heal.getId(),heal.getPrice())); shop.save(new ShopItem("Arcane Strike",strike.getId(),strike.getPrice()));
            }
            if (puzzles.count() == 0) puzzles.save(new Puzzle("[[\"bK\",null,null,null,null,null,null,null],[null,null,null,null,null,null,null,null],[null,null,null,null,null,null,null,null],[null,null,null,null,null,null,null,null],[null,null,null,null,null,null,null,null],[null,null,null,null,null,null,null,null],[null,null,null,null,\"wR\",null,null,null],[null,null,null,null,\"wK\",null,null,null]]","e2","e8",75));
        };
    }
}
