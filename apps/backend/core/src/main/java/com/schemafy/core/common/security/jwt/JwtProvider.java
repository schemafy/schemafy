package com.schemafy.core.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Component
public class JwtProvider {

    public static final String ACCESS_TOKEN = "ACCESS";
    public static final String REFRESH_TOKEN = "REFRESH";
    private static final String CLAIM_TYPE   = "type";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtProvider(JwtProperties properties) {
        this.jwtProperties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String userId, Map<String, Object> claims, long now) {
        Map<String, Object> tokenClaims = new HashMap<>(claims);
        tokenClaims.put(CLAIM_TYPE, ACCESS_TOKEN);

        return Jwts.builder()
                .claims(tokenClaims)
                .subject(userId)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(new Date(now))
                .audience().add(jwtProperties.getAudience()).and()
                .expiration(new Date(now + jwtProperties.getAccessTokenExpiration()))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .claim(CLAIM_TYPE, REFRESH_TOKEN)
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpiration()))
                .signWith(secretKey)
                .compact();
    }

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean validateToken(String token, String userId) {
        try {
            Claims claims = extractAllClaims(token);

            // 정책 체크
            if (!Objects.equals(jwtProperties.getIssuer(), claims.getIssuer())) return false;
            if (claims.getAudience() == null || !claims.getAudience().contains(jwtProperties.getAudience())) return false;
            if (!Objects.equals(userId, claims.getSubject())) return false;
            if (claims.getExpiration() == null || isTokenExpired(token)) return false;
        } catch (Exception e) {
            return false;
        }

        final String extractedUserId = extractUserId(token);
        return extractedUserId.equals(userId);
    }

    public String getTokenType(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_TYPE, String.class));
    }
}