package in.bank.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import in.bank.security.JwtService;
import in.bank.security.CustomUserDetailsService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // ✅ If no token → continue (public APIs allowed)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // ✅ Extract username from token
            String username = jwtService.extractUsername(token);

            // ✅ If user not authenticated yet
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // ✅ Validate token
                if (jwtService.validateToken(token, userDetails)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    token, // store token in credentials
                                    userDetails.getAuthorities()
                            );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            // ✅ Continue request
            filterChain.doFilter(request, response);

        }

        // ================= JWT EXCEPTION HANDLING =================

        catch (io.jsonwebtoken.ExpiredJwtException ex) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            response.getWriter().write("""
                {
                  "status": "ERROR",
                  "message": "JWT token has expired. Please login again.",
                  "code": "TOKEN_EXPIRED_401"
                }
            """);
        }

        catch (io.jsonwebtoken.JwtException ex) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            response.getWriter().write("""
                {
                  "status": "ERROR",
                  "message": "Invalid JWT token",
                  "code": "INVALID_TOKEN_401"
                }
            """);
        }

        catch (Exception ex) {

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");

            response.getWriter().write("""
                {
                  "status": "ERROR",
                  "message": "Something went wrong",
                  "code": "SERVER_500"
                }
            """);
        }
    }
}