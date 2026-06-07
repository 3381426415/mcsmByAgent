package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.SetupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/setup")
public class SetupController {

    @Autowired
    private SetupService setupService;

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        boolean complete = setupService.isSetupComplete();
        boolean dbReady = setupService.isDbConfigured();
        return Result.success("ok", Map.of("setupComplete", complete, "dbReady", dbReady));
    }

    @PostMapping("/admin")
    public Result<Map<String, Object>> createAdmin(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "admin");
        String password = body.get("password");

        if (password == null || password.length() < 6) {
            return Result.error("密码长度不能少于6位");
        }

        Map<String, Object> result = setupService.createAdmin(username, password);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success((String) result.get("message"), result);
        }
        return Result.error((String) result.get("message"));
    }

    @PostMapping("/complete")
    public Result<Map<String, Object>> complete() {
        Map<String, Object> result = setupService.writeConfig();
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success((String) result.get("message"), result);
        }
        return Result.error((String) result.get("message"));
    }
}
