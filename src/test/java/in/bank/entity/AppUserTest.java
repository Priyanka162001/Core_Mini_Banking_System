package in.bank.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppUser Entity Tests")
class AppUserTest {

    @Test
    @DisplayName("TC1: Builder creates AppUser with all fields set correctly")
    void testBuilder_AllFields() {
        LocalDateTime now = LocalDateTime.now();

        AppUser user = AppUser.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phoneNumber("9876543210")
                .countryCode("+91")
                .password("encodedPassword")
                .role(UserRole.ROLE_CUSTOMER)  // Changed from CUSTOMER to ROLE_CUSTOMER
                .status(UserStatus.ACTIVE)
                .kycStatus(KycStatus.FULL_KYC)  // Changed from VERIFIED to FULL_KYC
                .otp("123456")
                .otpExpiry(now.plusMinutes(5))
                .emailVerified(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(user.getPhoneNumber()).isEqualTo("9876543210");
        assertThat(user.getCountryCode()).isEqualTo("+91");
        assertThat(user.getPassword()).isEqualTo("encodedPassword");
        assertThat(user.getRole()).isEqualTo(UserRole.ROLE_CUSTOMER);  // Changed from CUSTOMER to ROLE_CUSTOMER
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getKycStatus()).isEqualTo(KycStatus.FULL_KYC);  // Changed from VERIFIED to FULL_KYC
        assertThat(user.getOtp()).isEqualTo("123456");
        assertThat(user.getEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("TC2: NoArgsConstructor creates empty AppUser")
    void testNoArgsConstructor() {
        AppUser user = new AppUser();
        assertThat(user).isNotNull();
        assertThat(user.getId()).isNull();
        assertThat(user.getEmail()).isNull();
    }

    @Test
    @DisplayName("TC3: AllArgsConstructor sets all fields")
    void testAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();

        AppUser user = new AppUser(
                1L, "John", "Doe", "john@example.com",
                "9876543210", "+91", "encodedPassword",
                UserRole.ROLE_CUSTOMER,  // Changed from CUSTOMER to ROLE_CUSTOMER
                UserStatus.ACTIVE,
                KycStatus.FULL_KYC,  // Changed from VERIFIED to FULL_KYC
                "123456", now,
                false, now, now
        );

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(user.getRole()).isEqualTo(UserRole.ROLE_CUSTOMER);  // Changed from CUSTOMER to ROLE_CUSTOMER
    }

    @Test
    @DisplayName("TC4: Setters update fields correctly")
    void testSetters() {
        AppUser user = new AppUser();

        user.setId(1L);
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setEmail("jane@example.com");
        user.setPhoneNumber("1234567890");
        user.setCountryCode("+1");
        user.setPassword("secret");
        user.setRole(UserRole.ROLE_ADMIN);  // Changed from ADMIN to ROLE_ADMIN
        user.setStatus(UserStatus.BLOCKED);  // Changed from INACTIVE to BLOCKED
        user.setKycStatus(KycStatus.MIN_KYC);  // Changed from PENDING to MIN_KYC
        user.setOtp("654321");
        user.setEmailVerified(true);

        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getEmail()).isEqualTo("jane@example.com");
        assertThat(user.getRole()).isEqualTo(UserRole.ROLE_ADMIN);  // Changed from ADMIN to ROLE_ADMIN
        assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);  // Changed from INACTIVE to BLOCKED
        assertThat(user.getKycStatus()).isEqualTo(KycStatus.MIN_KYC);  // Changed from PENDING to MIN_KYC
        assertThat(user.getEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("TC5: prePersist sets createdAt and updatedAt automatically")
    void testPrePersist_SetsTimestamps() {
        AppUser user = new AppUser();
        assertThat(user.getCreatedAt()).isNull();
        assertThat(user.getUpdatedAt()).isNull();

        // Simulate @PrePersist
        user.prePersist();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(user.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("TC6: preUpdate updates updatedAt timestamp")
    void testPreUpdate_UpdatesTimestamp() throws InterruptedException {
        AppUser user = new AppUser();
        user.prePersist();

        LocalDateTime createdAt = user.getCreatedAt();
        LocalDateTime originalUpdatedAt = user.getUpdatedAt();

        // Small delay to ensure timestamp difference
        Thread.sleep(10);

        // Simulate @PreUpdate
        user.preUpdate();

        assertThat(user.getCreatedAt()).isEqualTo(createdAt); // createdAt unchanged
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt); // updatedAt updated
    }

    @Test
    @DisplayName("TC7: Default emailVerified is false")
    void testDefaultEmailVerified() {
        AppUser user = new AppUser();
        // Default value set in field declaration
        assertThat(user.getEmailVerified()).isFalse(); // null before any set
    }

    @Test
    @DisplayName("TC8: UserRole enum values are valid")
    void testUserRoleEnum() {
        AppUser user = AppUser.builder()
                .role(UserRole.ROLE_CUSTOMER)  // Changed from CUSTOMER to ROLE_CUSTOMER
                .build();
        assertThat(user.getRole()).isEqualTo(UserRole.ROLE_CUSTOMER);  // Changed from CUSTOMER to ROLE_CUSTOMER

        user.setRole(UserRole.ROLE_ADMIN);  // Changed from ADMIN to ROLE_ADMIN
        assertThat(user.getRole()).isEqualTo(UserRole.ROLE_ADMIN);  // Changed from ADMIN to ROLE_ADMIN
    }

    @Test
    @DisplayName("TC9: UserStatus enum values are valid")
    void testUserStatusEnum() {
        AppUser user = AppUser.builder().status(UserStatus.PENDING).build();
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING);

        user.setStatus(UserStatus.ACTIVE);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);

        user.setStatus(UserStatus.BLOCKED);  // Changed from INACTIVE to BLOCKED
        assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);  // Changed from INACTIVE to BLOCKED
    }

    @Test
    @DisplayName("TC10: KycStatus enum values are valid")
    void testKycStatusEnum() {
        AppUser user = AppUser.builder().kycStatus(KycStatus.MIN_KYC).build();  // Changed from PENDING to MIN_KYC
        assertThat(user.getKycStatus()).isEqualTo(KycStatus.MIN_KYC);  // Changed from PENDING to MIN_KYC

        user.setKycStatus(KycStatus.FULL_KYC);  // Changed from VERIFIED to FULL_KYC
        assertThat(user.getKycStatus()).isEqualTo(KycStatus.FULL_KYC);  // Changed from VERIFIED to FULL_KYC
    }

    @Test
    @DisplayName("TC11: OTP expiry can be set and retrieved")
    void testOtpExpiry() {
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(10);
        AppUser user = AppUser.builder()
                .otp("999888")
                .otpExpiry(expiry)
                .build();

        assertThat(user.getOtp()).isEqualTo("999888");
        assertThat(user.getOtpExpiry()).isEqualTo(expiry);
    }
}