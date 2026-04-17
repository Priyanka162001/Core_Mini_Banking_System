package in.bank.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SwaggerConfig Tests")
class SwaggerConfigTest {

    private final SwaggerConfig swaggerConfig = new SwaggerConfig();

    @Test
    @DisplayName("TC1: customOpenAPI creates OpenAPI object with correct configuration")
    void testCustomOpenAPI() {
        // When
        OpenAPI openAPI = swaggerConfig.customOpenAPI();
        
        // Then
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Mini Core Banking System");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(openAPI.getInfo().getDescription()).isEqualTo("Banking System with JWT Security");
        
        // Verify security
        assertThat(openAPI.getSecurity()).isNotEmpty();
        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        
        // Verify security scheme
        SecurityScheme securityScheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertThat(securityScheme).isNotNull();
        assertThat(securityScheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(securityScheme.getScheme()).isEqualTo("bearer");
        assertThat(securityScheme.getBearerFormat()).isEqualTo("JWT");
        
        // Verify tags
        assertThat(openAPI.getTags()).isNotNull();
        assertThat(openAPI.getTags()).hasSize(10);
        assertThat(openAPI.getTags().get(0).getName()).isEqualTo("Auth");
        assertThat(openAPI.getTags().get(1).getName()).isEqualTo("Customer Profile");
        assertThat(openAPI.getTags().get(2).getName()).isEqualTo("Customer Address");
        assertThat(openAPI.getTags().get(3).getName()).isEqualTo("KYC");
        assertThat(openAPI.getTags().get(4).getName()).isEqualTo("Savings Accounts");
        assertThat(openAPI.getTags().get(5).getName()).isEqualTo("Account Opening");
        assertThat(openAPI.getTags().get(6).getName()).isEqualTo("Transactions");
        assertThat(openAPI.getTags().get(7).getName()).isEqualTo("Savings Products");
        assertThat(openAPI.getTags().get(8).getName()).isEqualTo("Interest Posting");
        assertThat(openAPI.getTags().get(9).getName()).isEqualTo("Admin");
    }

    @Test
    @DisplayName("TC2: tagOrderCustomizer returns OpenApiCustomizer that orders tags correctly")
    void testTagOrderCustomizer() {
        // Given
        OpenAPI openAPI = new OpenAPI();
        // Create unordered tags
        openAPI.setTags(List.of(
            new Tag().name("Admin"),
            new Tag().name("Auth"),
            new Tag().name("Transactions")
        ));
        
        // When
        OpenApiCustomizer customizer = swaggerConfig.tagOrderCustomizer();
        customizer.customise(openAPI);
        
        // Then
        assertThat(openAPI.getTags()).isNotNull();
        assertThat(openAPI.getTags()).hasSize(10);
        assertThat(openAPI.getTags().get(0).getName()).isEqualTo("Auth");
        assertThat(openAPI.getTags().get(1).getName()).isEqualTo("Customer Profile");
        assertThat(openAPI.getTags().get(2).getName()).isEqualTo("Customer Address");
        assertThat(openAPI.getTags().get(3).getName()).isEqualTo("KYC");
        assertThat(openAPI.getTags().get(4).getName()).isEqualTo("Savings Accounts");
        assertThat(openAPI.getTags().get(5).getName()).isEqualTo("Account Opening");
        assertThat(openAPI.getTags().get(6).getName()).isEqualTo("Transactions");
        assertThat(openAPI.getTags().get(7).getName()).isEqualTo("Savings Products");
        assertThat(openAPI.getTags().get(8).getName()).isEqualTo("Interest Posting");
        assertThat(openAPI.getTags().get(9).getName()).isEqualTo("Admin");
    }

    @Test
    @DisplayName("TC3: tagOrderCustomizer handles null tags")
    void testTagOrderCustomizer_WithNullTags() {
        // Given
        OpenAPI openAPI = new OpenAPI();
        openAPI.setTags(null); // Initially null
        
        // When
        OpenApiCustomizer customizer = swaggerConfig.tagOrderCustomizer();
        customizer.customise(openAPI);
        
        // Then
        assertThat(openAPI.getTags()).isNotNull();
        assertThat(openAPI.getTags()).hasSize(10);
        assertThat(openAPI.getTags().get(0).getName()).isEqualTo("Auth");
    }

    @Test
    @DisplayName("TC4: All expected tags are present in correct order")
    void testAllTagsPresentInCorrectOrder() {
        List<String> expectedTagOrder = List.of(
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
        
        OpenAPI openAPI = swaggerConfig.customOpenAPI();
        
        List<String> actualTagNames = openAPI.getTags().stream()
            .map(Tag::getName)
            .toList();
        
        assertThat(actualTagNames).containsExactlyElementsOf(expectedTagOrder);
    }

    @Test
    @DisplayName("TC5: Security scheme has correct configuration")
    void testSecuritySchemeConfiguration() {
        OpenAPI openAPI = swaggerConfig.customOpenAPI();
        
        SecurityScheme securityScheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        
        assertThat(securityScheme.getName()).isEqualTo("Authorization");
        assertThat(securityScheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(securityScheme.getScheme()).isEqualTo("bearer");
        assertThat(securityScheme.getBearerFormat()).isEqualTo("JWT");
    }

    @Test
    @DisplayName("TC6: Info object has all required fields")
    void testInfoObject() {
        OpenAPI openAPI = swaggerConfig.customOpenAPI();
        Info info = openAPI.getInfo();
        
        assertThat(info.getTitle()).isNotBlank();
        assertThat(info.getVersion()).isNotBlank();
        assertThat(info.getDescription()).isNotBlank();
    }
}