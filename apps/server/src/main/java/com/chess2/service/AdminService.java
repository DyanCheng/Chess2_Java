package com.chess2.service;

import com.chess2.api.ApiException;
import com.chess2.domain.*;
import com.chess2.dto.AdminDtos;
import com.chess2.repository.*;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AdminService {
    private final UserRepository users;private final WalletRepository wallets;private final GameRepository games;private final FunctionCardRepository cards;private final UserCardRepository inventory;private final ShopItemRepository shop;private final AuditService audit;
    public AdminService(UserRepository users,WalletRepository wallets,GameRepository games,FunctionCardRepository cards,UserCardRepository inventory,ShopItemRepository shop,AuditService audit){this.users=users;this.wallets=wallets;this.games=games;this.cards=cards;this.inventory=inventory;this.shop=shop;this.audit=audit;}
    public AdminDtos.Overview overview(){
        var rows=users.findAll().stream().sorted(Comparator.comparing(UserAccount::getUsername)).map(this::row).toList();
        var shopRows=shop.findAll().stream().map(item->new AdminDtos.ShopRow(item.getId(),item.getCardId(),item.getName(),item.getPrice(),item.isActive())).toList();
        return new AdminDtos.Overview(users.count(),users.countByLockedAtIsNotNull(),games.count(),games.countByStatus(GameStatus.PLAYING),cards.count(),rows,shopRows);
    }
    @Transactional public AdminDtos.UserRow wallet(UUID id,AdminDtos.WalletRequest request){UserAccount user=user(id);Wallet wallet=wallets.findById(id).orElseGet(()->wallets.save(new Wallet(id)));wallet.setBalances(request.gold(),request.gems());return row(user);}
    @Transactional public AdminDtos.UserRow role(UUID actorId,UUID id,AdminDtos.RoleRequest request){if(actorId.equals(id)&&request.role().equals("PLAYER"))throw new ApiException(HttpStatus.CONFLICT,"Bạn không thể tự gỡ quyền admin của chính mình");UserAccount user=user(id);user.setRole(Role.valueOf(request.role()));return row(user);}
    @Transactional public AdminDtos.UserRow lock(UUID actorId,UUID id,AdminDtos.LockRequest request){if(actorId.equals(id)&&request.locked())throw new ApiException(HttpStatus.CONFLICT,"Bạn không thể tự khóa tài khoản đang đăng nhập");UserAccount user=user(id);String reason=request.reason()==null||request.reason().isBlank()?"Khóa bởi quản trị viên":request.reason().trim();user.setLocked(request.locked(),reason,actorId);return row(user);}
    @Transactional public void grantCard(UUID id,AdminDtos.GrantCardRequest request){user(id);cards.findById(request.cardId()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Card not found"));UserCard owned=inventory.findByUserIdAndCardId(id,request.cardId()).orElseGet(()->new UserCard(id,request.cardId(),0));owned.add(request.quantity());inventory.save(owned);}
    @Transactional public AdminDtos.ShopRow shop(UUID id,AdminDtos.ShopRequest request){ShopItem item=shop.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Shop item not found"));item.update(request.price(),request.active());return new AdminDtos.ShopRow(item.getId(),item.getCardId(),item.getName(),item.getPrice(),item.isActive());}
    public java.util.List<AdminDtos.AuditRow> logs(UUID userId,int limit){return audit.latest(userId,limit);}
    private UserAccount user(UUID id){return users.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"User not found"));}
    private AdminDtos.UserRow row(UserAccount user){Wallet wallet=wallets.findById(user.getId()).orElseGet(()->new Wallet(user.getId()));return new AdminDtos.UserRow(user.getId(),user.getUsername(),user.getEmail(),user.getRole().name(),user.getAccountType().name(),user.getRating(),user.getWins(),user.getLosses(),wallet.getGold(),wallet.getGems(),user.getCreatedAt(),!user.isAccountNonLocked(),user.getLockedAt(),user.getLockedReason());}
}
