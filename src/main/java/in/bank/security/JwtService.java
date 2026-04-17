package in.bank.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

    private static final String SECRET = "my-super-secret-key-my-super-secret-key";
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    // Access token → 15 minutes
    private static final long JWT_EXPIRATION = 1000 * 60 * 15;

    // Refresh token → 7 days
    private static final long REFRESH_EXPIRATION = 1000 * 60 * 60 * 24 * 7;

    // ─────────────────────────────────────────────
    // GENERATE ACCESS TOKEN (userId + role + email)
    // ─────────────────────────────────────────────
    public String generateToken(UserDetails userDetails, Long userId) {

        Map<String, Object> claims = new HashMap<>();

        String role = userDetails.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        claims.put("role", role.replace("ROLE_", "")); // stored as "ADMIN" or "CUSTOMER"
        claims.put("email", userDetails.getUsername());
        claims.put("userId", userId);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    // ─────────────────────────────────────────────
    // GENERATE REFRESH TOKEN
    // ─────────────────────────────────────────────
    public String generateRefreshToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    // ─────────────────────────────────────────────
    // GET LOGGED-IN USER ID FROM SECURITY CONTEXT
    // ─────────────────────────────────────────────
    public Long getLoggedInUserId() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found in security context");
        }

        Object credentials = authentication.getCredentials();

        if (credentials instanceof String token) {
            return extractUserId(token);
        }

        throw new RuntimeException(
            "JWT token not found in security context credentials. " +
            "Ensure JwtFilter passes token (not null) as credentials."
        );
    }

    // ─────────────────────────────────────────────
    // EXTRACT USERNAME (email)
    // ─────────────────────────────────────────────
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // ─────────────────────────────────────────────
    // EXTRACT SINGLE ROLE → returns "ROLE_ADMIN" or "ROLE_CUSTOMER"
    // ─────────────────────────────────────────────
    public String extractRole(String token) {
        Object role = extractAllClaims(token).get("role");
        if (role == null) throw new RuntimeException("Role not found in token");
        return "ROLE_" + role.toString();
    }

    // ─────────────────────────────────────────────
    // EXTRACT ROLES AS LIST → used by isAdmin() in controller
    // e.g. contains("ROLE_ADMIN") or contains("ROLE_CUSTOMER")
    // ─────────────────────────────────────────────
    public List<String> extractRoles(String token) {
        return List.of(extractRole(token)); // single role wrapped in a list
    }

    // ─────────────────────────────────────────────
    // EXTRACT USER ID
    // ─────────────────────────────────────────────
    public Long extractUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        if (userId == null) throw new RuntimeException("User ID not found in token");
        return ((Number) userId).longValue();
    }

    // ─────────────────────────────────────────────
    // EXTRACT EXPIRATION
    // ─────────────────────────────────────────────
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    // ─────────────────────────────────────────────
    // VALIDATE TOKEN
    // ─────────────────────────────────────────────
    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────
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