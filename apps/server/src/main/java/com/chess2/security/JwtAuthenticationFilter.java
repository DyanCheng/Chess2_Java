package com.chess2.security;

import io.jsonwebtoken.JwtException;
import com.chess2.domain.UserAccount;
import com.chess2.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt; private final UserRepository users;
    public JwtAuthenticationFilter(JwtService jwt, UserRepository users) { this.jwt=jwt; this.users=users; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String token = token(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UUID userId = UUID.fromString(jwt.parse(token).getSubject());
                UserAccount user = users.findById(userId).orElse(null);
                if (user == null || !user.isAccountNonLocked()) { chain.doFilter(request, response); return; }
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
            } catch (JwtException | IllegalArgumentException ignored) { }
        }
        chain.doFilter(request, response);
    }
    private String token(HttpServletRequest request) {
        String authorization=request.getHeader("Authorization");
        if(authorization!=null&&authorization.startsWith("Bearer "))return authorization.substring(7).trim();
        if(!"/ws/games".equals(request.getServletPath())||!"websocket".equalsIgnoreCase(request.getHeader("Upgrade")))return null;
        String protocols=request.getHeader("Sec-WebSocket-Protocol");
        if(protocols==null)return null;
        for(String protocol:protocols.split(",")){String value=protocol.trim();if(value.startsWith("jwt.")&&value.length()>4)return value.substring(4);}
        return null;
    }
}
