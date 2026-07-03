package com.unopower.userservice.service;

import com.unopower.userservice.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtIssuer {

    private final JwtProperties jwtProperties;

    public JwtIssuer(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String issueFor(String userId, String username) {
        return issueFor(userId, username, false);
    }

    public String issueFor(String userId, String username, boolean guest) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpirationMs());

        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("guest", guest)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }
}
