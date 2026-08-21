package com.sp.api.common.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    /** HS256 서명에 필요한 최소 키 길이(바이트). */
    private static final int MIN_SECRET_BYTES = 32;

    private SecretKey key;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @PostConstruct
    public void init() {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret 은 최소 " + MIN_SECRET_BYTES + "바이트여야 합니다. 현재: " + keyBytes.length
            );
        }

        key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(String email) {

        Date now = new Date();

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    /**
     * 서명과 만료를 검증하고 subject(email)를 돌려준다.
     * 유효하지 않으면 null 을 반환해 호출부가 인증을 건너뛰도록 한다.
     */
    public String parseEmail(String token) {

        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();

        } catch (ExpiredJwtException e) {
            log.debug("만료된 토큰");
            return null;

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("유효하지 않은 토큰: {}", e.getMessage());
            return null;
        }
    }
}
