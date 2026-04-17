package in.bank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;

@Configuration
public class SwaggerConfig {

    // ✅ Define your desired order here
    private static final List<String> TAG_ORDER = List.of(
            "Auth",
            "Customer Profile",
            "Customer Address",
            "KYC",
            "Savings Accounts",
            "Account Opening",
            "Transactions",
            "Savings Products",
            "Interest Posting",
            "Admin"
    );

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Mini Core Banking System")
                        .version("1.0")
                        .description("Banking System with JWT Security"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .tags(TAG_ORDER.stream()
                        .map(name -> new Tag().name(name))
                        .toList());
    }

    // ✅ This customizer FORCES the tag order at runtime
    @Bean
    public OpenApiCustomizer tagOrderCustomizer() {
        return openApi -> {
            List<Tag> ordered = TAG_ORDER.stream()
                    .map(name -> new Tag().name(name))
                    .toList();
            openApi.setTags(ordered);
        };
    }
}