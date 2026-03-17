package in.bank.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final String SECRET = "my-super-secret-key-my-super-secret-key";
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    // Access token → 15 minutes
    private static final long JWT_EXPIRATION = 1000 * 60 * 15;

    // Refresh token → 7 days
    private static final long REFRESH_EXPIRATION = 1000 * 60 * 60 * 24 * 7;

    // ACCESS TOKEN
    // ✅ Now includes userId
    public String generateToken(UserDetails userDetails, Long userId) {

        Map<String, Object> claims = new HashMap<>();

        String role = userDetails.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        claims.put("role", role.replace("ROLE_", ""));
        claims.put("email", userDetails.getUsername());
        claims.put("userId", userId); // ✅ Add userId claim

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    // REFRESH TOKEN
    public String generateRefreshToken(UserDetails userDetails) {

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract username
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Extract role
    public String extractRole(String token) {
        return "ROLE_" + extractAllClaims(token).get("role", String.class);
    }

    // ✅ Extract userId
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userId = claims.get("userId");
        if (userId == null) throw new RuntimeException("User ID not found in token");
        return Long.valueOf(userId.toString());
    }

    // Token expiration
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}