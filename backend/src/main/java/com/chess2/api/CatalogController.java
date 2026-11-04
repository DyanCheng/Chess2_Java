package com.chess2.api;

import com.chess2.domain.*;
import com.chess2.service.CatalogService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalog;
    public CatalogController(CatalogService catalog) { this.catalog=catalog; }
    @GetMapping("/cards") public List<FunctionCard> cards() { return catalog.cards(); }
    @GetMapping("/cards/inventory") public List<UserCard> inventory(@AuthenticationPrincipal UserAccount user) { return catalog.inventory(user); }
    @GetMapping("/shop") public List<ShopItem> shop() { return catalog.shop(); }
    @PostMapping("/shop/{itemId}/purchase") @ResponseStatus(HttpStatus.NO_CONTENT) public void purchase(@AuthenticationPrincipal UserAccount user,@PathVariable UUID itemId) { catalog.purchase(user,itemId); }
}
