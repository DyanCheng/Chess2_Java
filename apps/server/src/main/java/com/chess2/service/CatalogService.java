package com.chess2.service;

import com.chess2.api.ApiException;
import com.chess2.domain.*;
import com.chess2.repository.*;
import jakarta.transaction.Transactional;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
    private static final List<String> STARTER_CARD_CODES=List.of("HEAL_20","ARCANE_STRIKE","FREEZE");
    private final FunctionCardRepository cards; private final UserCardRepository userCards; private final ShopItemRepository shop; private final WalletRepository wallets;
    public CatalogService(FunctionCardRepository cards, UserCardRepository userCards, ShopItemRepository shop, WalletRepository wallets) { this.cards=cards; this.userCards=userCards; this.shop=shop; this.wallets=wallets; }
    public List<FunctionCard> cards() { return cards.findAll(); }
    public FunctionCard card(UUID cardId) { return cards.findById(cardId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Card not found")); }
    public List<UserCard> inventory(UserAccount user) { return userCards.findByUserId(user.getId()); }
    public List<UUID> deck(UserAccount user) { return userCards.findByUserId(user.getId()).stream().filter(card -> card.getDeckSlot()!=null && card.getQuantity()>0).sorted(java.util.Comparator.comparing(UserCard::getDeckSlot)).map(UserCard::getCardId).toList(); }
    @Transactional public void grantStarterDeck(UserAccount user) {
        if(!userCards.findByUserId(user.getId()).isEmpty())return;
        int slot=1;
        for(String code:STARTER_CARD_CODES){FunctionCard card=cards.findByCode(code).orElseThrow(()->new IllegalStateException("Missing starter card: "+code));UserCard starter=new UserCard(user.getId(),card.getId(),1);starter.equip(slot++);userCards.save(starter);}
    }
    @Transactional public List<UserCard> equipDeck(UserAccount user, List<UUID> cardIds) {
        Set<UUID> unique=new LinkedHashSet<>(cardIds);
        if(unique.size()!=cardIds.size())throw new ApiException(HttpStatus.BAD_REQUEST,"Mỗi loại thẻ chỉ được trang bị một lần");
        if(unique.size()>3)throw new ApiException(HttpStatus.BAD_REQUEST,"Deck chỉ có tối đa 3 thẻ");
        List<UserCard> inventory=userCards.findByUserId(user.getId());
        for(UUID cardId:unique)if(inventory.stream().noneMatch(card->card.getCardId().equals(cardId)&&card.getQuantity()>0))throw new ApiException(HttpStatus.CONFLICT,"Deck chỉ được dùng thẻ đang sở hữu");
        inventory.forEach(card->card.equip(null));
        userCards.flush();
        int slot=1;
        for(UUID cardId:unique){int selectedSlot=slot++;inventory.stream().filter(card->card.getCardId().equals(cardId)).findFirst().orElseThrow().equip(selectedSlot);}
        return userCards.saveAll(inventory);
    }
    public List<ShopItem> shop() { return shop.findByActiveTrue(); }
    @Transactional public void purchase(UserAccount user, UUID itemId) {
        ShopItem item = shop.findById(itemId).filter(ShopItem::isActive).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Shop item not found"));
        Wallet wallet = wallets.findById(user.getId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
        wallet.debitGold(item.getPrice());
        if (item.getCardId() != null) {
            List<UserCard> owned=userCards.findByUserId(user.getId());
            UserCard inventory=owned.stream().filter(card->card.getCardId().equals(item.getCardId())).findFirst().orElseGet(()->new UserCard(user.getId(),item.getCardId(),0));
            inventory.add(1);
            if(inventory.getDeckSlot()==null){Set<Integer> used=owned.stream().map(UserCard::getDeckSlot).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());for(int slot=1;slot<=3;slot++)if(!used.contains(slot)){inventory.equip(slot);break;}}
            userCards.save(inventory);
        }
    }
    @Transactional public FunctionCard useCard(UserAccount user, UUID cardId) {
        UserCard inventory = userCards.findByUserIdAndCardId(user.getId(), cardId).orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Card is not owned")); inventory.use();
        return cards.findById(cardId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Card not found"));
    }
}
