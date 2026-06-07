package com.gitcode.mcsm_backend.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import jakarta.servlet.ServletException;

import com.gitcode.mcsm_backend.service.userDetailsService;

/**
 * JWT 过滤器 - 从请求头提取 Token，验证并设置 Spring Security 上下文
 */
@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;

    @Autowired
    private userDetailsService userDetailsService;

    public JwtFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {






        // 1. 核心修复：如果是 OPTIONS 请求，直接放行，不走任何拦截逻辑
        // 这样可以确保 SecurityConfig 里的跨域配置能顺利返回给浏览器
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }


        // 2. 兼容性修复：同时尝试从 'token' 和 'Authorization' 中获取
        String token = request.getHeader("token");
        String authHeader = request.getHeader("Authorization");

        if (token == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // 校验 Token
        if (token != null && !token.isEmpty()) {
            try {
                String username = jwtUtils.getUsernameFromToken(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // ★ 核心修复：使用 userDetailsService 加载完整用户信息（含权限）
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()  // ← 修复：传入真实权限
                            );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                log.info("JWT 解析失败: {}", e.getMessage());
            }
        }

        // 继续过滤器链
        chain.doFilter(request, response);
    }
}