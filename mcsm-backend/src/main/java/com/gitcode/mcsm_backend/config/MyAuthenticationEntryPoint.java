package com.gitcode.mcsm_backend.config;

import com.gitcode.mcsm_backend.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 认证失败处理 - 浏览器访问跳转首页，AJAX 请求返回 401 JSON
 */
@Component
public class MyAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String accept = request.getHeader("Accept");
        String xhr = request.getHeader("X-Requested-With");

        // 浏览器直接访问页面 → 跳转首页，由前端路由处理登录
        if (accept != null && accept.contains("text/html") && !"XMLHttpRequest".equals(xhr)) {
            response.sendRedirect("/");
            return;
        }

        // AJAX / API 请求 → 返回 401 JSON
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        Result<Object> result = Result.noPermission("登录凭证已过期或无效，请重新登录");
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result);
        response.getWriter().write(json);
    }
}