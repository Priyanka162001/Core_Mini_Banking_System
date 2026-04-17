package in.bank.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import in.bank.security.CustomUserDetails;

import java.util.Optional;

@EnableJpaAuditing(auditorAwareRef = "auditorProvider") // ✅ must match your @Bean method name
@Configuration
@RequiredArgsConstructor
public class AuditorConfig {

    // ✅ Remove UserRepository entirely — no DB call needed

	@Bean
	public AuditorAware<Long> auditorProvider() {
	    return () -> {
	        var auth = SecurityContextHolder.getContext().getAuthentication();

	        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
	            return Optional.empty();
	        }

	        Object principal = auth.getPrincipal();

	        if (principal instanceof CustomUserDetails userDetails) {
	            return Optional.of(userDetails.getId()); // ✅ NOW WORKS
	        }

	        return Optional.empty();
	    };
	}
}