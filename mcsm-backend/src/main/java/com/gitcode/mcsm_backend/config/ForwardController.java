package com.gitcode.mcsm_backend.config;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Vue Router history 模式兜底 — 将前端路由转发到 index.html
 */
@Configuration
public class ForwardController {

    @GetMapping({"/register", "/baned", "/userPanel/**", "/adminPanel/**"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }

    /**
     * 404 错误转发到 index.html，支持 Vue Router history 模式
     */
    @Bean
    public ErrorPageRegistrar errorPageRegistrar() {
        return (ErrorPageRegistry registry) -> {
            registry.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/index.html"));
        };
    }
}
