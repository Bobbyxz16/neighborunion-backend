package com.example.neighborhelp.security;

import com.example.neighborhelp.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;


@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String jwtSecret;
    private final long EXPIRATION_TIME = 1000 * 60 * 60;

    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken (User user){
        return Jwts.builder()
                .setSubject(user.getUsername()) // identify the principal user
                .claim("role", user.getRole().name()) // Confirm information about the user
                .claim("userId", user.getId())
                .setIssuedAt(new Date()) // Register when was the token created
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // Calculate the expiration time (current time + expiration time)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Convert the secret key into valid key sign
                .compact();

    }

    public boolean validateToken(String token){
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey()) // Prepare validation with foreign key
                    .build()
                    .parseClaimsJws(token);
                    return true;
        } catch (JwtException | IllegalArgumentException e){
            return false;
        }
    }

    public String extractUsername(String token){
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public Long extractUserId (String token){
        return ((Number) Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("userId")).longValue();

    }

    public boolean isTokenValid(String token, UserDetails userDetails){
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver){
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token){
        return Jwts
                .parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

    }

    public long getExpirationTime() {
        return EXPIRATION_TIME / 1000;
    }



}
