package com.telehealth.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expirationMs}")
    private int jwtExpirationMs;

    public String generateDpopBoundToken(Authentication authentication, String dpopHeader, List<String> amr) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        String realUserId = userPrincipal.getId().toString();

        // TODO: розшифровувати реальний dpopHeader
        String jwkThumbprint = Integer.toHexString(dpopHeader.hashCode());

        return Jwts.builder()
                .setSubject(realUserId)
                .claim("amr", amr)
                .claim("cnf", Map.of("jkt", jwkThumbprint))
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
