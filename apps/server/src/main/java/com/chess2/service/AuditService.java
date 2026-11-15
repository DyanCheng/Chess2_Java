package com.chess2.service;

import com.chess2.domain.AuditEvent;
import com.chess2.domain.UserAccount;
import com.chess2.dto.AdminDtos;
import com.chess2.repository.AuditEventRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditEventRepository events;
    public AuditService(AuditEventRepository events) { this.events = events; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UserAccount actor, String method, String path, int status) {
        events.save(new AuditEvent(actor.getId(), actor.getUsername(), category(path), method, path, status));
    }

    public List<AdminDtos.AuditRow> latest(UUID actorUserId, int limit) {
        var page = PageRequest.of(0, Math.min(Math.max(limit, 1), 200), Sort.by(Sort.Direction.DESC, "occurredAt"));
        var result = actorUserId == null ? events.findAll(page) : events.findByActorUserId(actorUserId, page);
        return result.stream().map(event -> new AdminDtos.AuditRow(
                event.getId(), event.getActorUserId(), event.getActorUsername(), event.getCategory(),
                action(event.getMethod(), event.getPath()), event.getMethod(), event.getPath(),
                event.getStatus(), event.getOccurredAt())).toList();
    }

    private static String category(String path) {
        if (path.startsWith("/api/admin")) return "ADMIN";
        if (path.startsWith("/api/games")) return "GAME";
        if (path.startsWith("/api/shop")) return "SHOP";
        if (path.startsWith("/api/cards")) return "INVENTORY";
        if (path.startsWith("/api/puzzles")) return "PUZZLE";
        if (path.startsWith("/api/auth")) return "AUTH";
        return "SYSTEM";
    }

    private static String action(String method, String path) {
        if ("/api/auth/login".equals(path)) return "Đăng nhập";
        if (path.endsWith("/moves")) return "Gửi nước đi";
        if (path.endsWith("/resign")) return "Đầu hàng";
        if (path.contains("/rooms/") && path.endsWith("/join")) return "Vào phòng PvP";
        if (path.endsWith("/quick-match")) return "Tìm trận nhanh";
        if ("POST".equals(method) && "/api/games".equals(path)) return "Tạo ván đấu";
        if (path.startsWith("/api/games/") && path.endsWith("/cards")) return "Dùng thẻ chức năng";
        if (path.startsWith("/api/shop/") && path.endsWith("/purchase")) return "Mua vật phẩm";
        if ("/api/cards/deck".equals(path)) return "Cập nhật bộ thẻ";
        if (path.startsWith("/api/puzzles/") && path.endsWith("/submit")) return "Giải puzzle";
        if (path.startsWith("/api/admin/users/") && path.endsWith("/lock")) return "Khóa hoặc mở tài khoản";
        if (path.startsWith("/api/admin")) return "Thao tác quản trị";
        return method + " " + path;
    }
}
