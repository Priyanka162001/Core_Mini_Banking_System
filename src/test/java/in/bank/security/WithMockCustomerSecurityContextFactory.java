package in.bank.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class WithMockCustomerSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomer> {
    
    @Override
    public SecurityContext createSecurityContext(WithMockCustomer annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        
        // Create a principal that has an 'id' property with getter
        TestCustomerPrincipal principal = new TestCustomerPrincipal(
            annotation.id(),
            annotation.username(),
            annotation.password(),
            annotation.roles()
        );
        
        Collection<? extends GrantedAuthority> authorities = Arrays.stream(annotation.roles())
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());
        
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, annotation.password(), authorities);
        context.setAuthentication(auth);
        return context;
    }
    
    /**
     * Principal class with getId() method that Spring Security can access
     */
    public static class TestCustomerPrincipal {
        private final Long id;
        private final String username;
        private final String password;
        private final String[] roles;
        
        public TestCustomerPrincipal(Long id, String username, String password, String[] roles) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.roles = roles;
        }
        
        public Long getId() {
            return id;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public String[] getRoles() {
            return roles;
        }
        
        @Override
        public String toString() {
            return username;
        }
    }
}