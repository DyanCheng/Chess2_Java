package com.chess2.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;

public final class AdminDtos {
    private AdminDtos() { }
    public record Overview(long users,long games,long activeGames,long cards,List<UserRow> recentUsers,List<ShopRow> shop) { }
    public record UserRow(UUID id,String username,String email,String role,String accountType,int rating,int wins,int losses,long gold,long gems) { }
    public record ShopRow(UUID id,UUID cardId,String name,long price,boolean active) { }
    public record WalletRequest(@Min(0) long gold,@Min(0) long gems) { }
    public record RoleRequest(@NotNull @Pattern(regexp="PLAYER|ADMIN") String role) { }
    public record GrantCardRequest(@NotNull UUID cardId,@Min(1) int quantity) { }
    public record ShopRequest(@Min(0) long price,boolean active) { }
}
