package com.chess2.security;

import com.chess2.domain.UserAccount;
import com.chess2.service.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuditRequestFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AuditRequestFilter.class);
    private final AuditService audit;
    public AuditRequestFilter(AuditService audit) { this.audit = audit; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return "GET".equals(request.getMethod()) || "OPTIONS".equals(request.getMethod())
                || !request.getRequestURI().startsWith("/api/");
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserAccount actor = authentication != null && authentication.getPrincipal() instanceof UserAccount user ? user : null;
        try {
            chain.doFilter(request, response);
        } finally {
            if (actor != null) {
                try { audit.record(actor, request.getMethod(), request.getRequestURI(), response.getStatus()); }
                catch (RuntimeException error) { log.warn("Could not persist audit event for {}", request.getRequestURI(), error); }
            }
        }
    }
}
