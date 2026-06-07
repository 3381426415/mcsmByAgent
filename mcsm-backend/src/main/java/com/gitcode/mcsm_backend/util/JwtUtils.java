package com.gitcode.mcsm_backend.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类 - 生成、解析、验证 JWT Token
 */
@Component
public class JwtUtils {

    @Value("${jwt.secret:#{T(java.util.UUID).randomUUID().toString() + T(java.util.UUID).randomUUID().toString()}}")
    private String signKey;

    @Value("${jwt.expire:43200000}")
    private long expire;

    public String createToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + expire))
                .signWith(SignatureAlgorithm.HS256, signKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(signKey)
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

}