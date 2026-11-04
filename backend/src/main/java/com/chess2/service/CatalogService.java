package com.chess2.service;

import com.chess2.api.ApiException;
import com.chess2.domain.*;
import com.chess2.repository.*;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
    private final FunctionCardRepository cards; private final UserCardRepository userCards; private final ShopItemRepository shop; private final WalletRepository wallets;
    public CatalogService(FunctionCardRepository cards, UserCardRepository userCards, ShopItemRepository shop, WalletRepository wallets) { this.cards=cards; this.userCards=userCards; this.shop=shop; this.wallets=wallets; }
    public List<FunctionCard> cards() { return cards.findAll(); }
    public List<UserCard> inventory(UserAccount user) { return userCards.findByUserId(user.getId()); }
    public List<ShopItem> shop() { return shop.findByActiveTrue(); }
    @Transactional public void purchase(UserAccount user, UUID itemId) {
        ShopItem item = shop.findById(itemId).filter(ShopItem::isActive).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Shop item not found"));
        Wallet wallet = wallets.findById(user.getId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
        wallet.debitGold(item.getPrice());
        if (item.getCardId() != null) { UserCard inventory = userCards.findByUserIdAndCardId(user.getId(), item.getCardId()).orElseGet(() -> new UserCard(user.getId(), item.getCardId(), 0)); inventory.add(1); userCards.save(inventory); }
    }
    @Transactional public FunctionCard useCard(UserAccount user, UUID cardId) {
        UserCard inventory = userCards.findByUserIdAndCardId(user.getId(), cardId).orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Card is not owned")); inventory.use();
        return cards.findById(cardId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Card not found"));
    }
}
