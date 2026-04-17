package in.bank.security;

import org.springframework.security.test.context.support.WithSecurityContext;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomerSecurityContextFactory.class)
public @interface WithMockCustomer {
    long id() default 1L;
    String username() default "customer";
    String[] roles() default {"CUSTOMER"};
    String password() default "password";
}