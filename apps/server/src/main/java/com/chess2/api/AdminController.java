package com.chess2.api;

import com.chess2.domain.UserAccount;
import com.chess2.dto.AdminDtos;
import com.chess2.service.AdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminController {
    private final AdminService admin;public AdminController(AdminService admin){this.admin=admin;}
    @GetMapping("/overview") public AdminDtos.Overview overview(){return admin.overview();}
    @PatchMapping("/users/{id}/wallet") public AdminDtos.UserRow wallet(@PathVariable UUID id,@Valid @RequestBody AdminDtos.WalletRequest request){return admin.wallet(id,request);}
    @PatchMapping("/users/{id}/role") public AdminDtos.UserRow role(@AuthenticationPrincipal UserAccount actor,@PathVariable UUID id,@Valid @RequestBody AdminDtos.RoleRequest request){return admin.role(actor.getId(),id,request);}
    @PatchMapping("/users/{id}/lock") public AdminDtos.UserRow lock(@AuthenticationPrincipal UserAccount actor,@PathVariable UUID id,@Valid @RequestBody AdminDtos.LockRequest request){return admin.lock(actor.getId(),id,request);}
    @PostMapping("/users/{id}/cards") public void grant(@PathVariable UUID id,@Valid @RequestBody AdminDtos.GrantCardRequest request){admin.grantCard(id,request);}
    @PatchMapping("/shop/{id}") public AdminDtos.ShopRow shop(@PathVariable UUID id,@Valid @RequestBody AdminDtos.ShopRequest request){return admin.shop(id,request);}
    @GetMapping("/logs") public List<AdminDtos.AuditRow> logs(@RequestParam(required=false) UUID userId,@RequestParam(defaultValue="100") @Min(1) @Max(200) int limit){return admin.logs(userId,limit);}
}
