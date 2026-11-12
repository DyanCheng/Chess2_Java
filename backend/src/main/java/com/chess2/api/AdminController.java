package com.chess2.api;

import com.chess2.domain.UserAccount;
import com.chess2.dto.AdminDtos;
import com.chess2.service.AdminService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService admin;public AdminController(AdminService admin){this.admin=admin;}
    @GetMapping("/overview") public AdminDtos.Overview overview(){return admin.overview();}
    @PatchMapping("/users/{id}/wallet") public AdminDtos.UserRow wallet(@PathVariable UUID id,@Valid @RequestBody AdminDtos.WalletRequest request){return admin.wallet(id,request);}
    @PatchMapping("/users/{id}/role") public AdminDtos.UserRow role(@AuthenticationPrincipal UserAccount actor,@PathVariable UUID id,@Valid @RequestBody AdminDtos.RoleRequest request){return admin.role(actor.getId(),id,request);}
    @PostMapping("/users/{id}/cards") public void grant(@PathVariable UUID id,@Valid @RequestBody AdminDtos.GrantCardRequest request){admin.grantCard(id,request);}
    @PatchMapping("/shop/{id}") public AdminDtos.ShopRow shop(@PathVariable UUID id,@Valid @RequestBody AdminDtos.ShopRequest request){return admin.shop(id,request);}
}
