package com.zerobase.dividend.security;

import com.zerobase.dividend.service.MemberService;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

import static io.jsonwebtoken.SignatureAlgorithm.*;

@Component
@RequiredArgsConstructor
public class TokenProvider {

    private static final String KEY_ROLES = "roles";
    private static final long TOKEN_EXPIRE_TIME = 1000 * 60 * 60; // 1 hour

    private final MemberService memberService;

    @Value("{spring.jwt.secret}")
    private String secretKey;

    /**
     * 토큰 생성 (발급) 메소드
     * @param username
     * @param roles
     * @return
     */
    public String generateToken(String username, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put(KEY_ROLES, roles);

        var now = new Date();
        var expiredDate = new Date(now.getTime() + TOKEN_EXPIRE_TIME);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiredDate)
                .signWith(HS512, this.secretKey)
                .compact();
    }

    public Authentication getAuthentication(String jwt) {
        UserDetails userDetails = this.memberService.loadUserByUsername(this.getUserName(jwt));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String getUserName(String token) {
        return this.parseClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        var claims = this.parseClaims(token);
        return !claims.getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser().setSigningKey(this.secretKey).parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
