package in.bank.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableMethodSecurity(prePostEnabled = true)
public class TestSecurityConfig {

    @Order(1)
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth ->
                auth
                    // ✅ Permit all auth endpoints (public)
                    .requestMatchers(
                        "/api/v1/auth/customers/register",
                        "/api/v1/auth/otp/verify",
                        "/api/v1/auth/otp/resend",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh"
                    ).permitAll()
                    // All other endpoints require authentication
                    .anyRequest().authenticated()
            )
            .httpBasic(basic ->
                basic.authenticationEntryPoint(
                    (request, response, ex) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                )
            );
        return http.build();
    }

    @Bean
    @Primary
    public UserDetailsService userDetailsService() {
        var customer = User.builder()
                .username("customer@test.com")
                .password("{noop}password")
                .roles("CUSTOMER")
                .build();
        var admin = User.builder()
                .username("admin@test.com")
                .password("{noop}password")
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(customer, admin);
    }
}