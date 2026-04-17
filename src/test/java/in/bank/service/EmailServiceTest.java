package in.bank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Nested
    @DisplayName("sendOtp() Tests")
    class SendOtpTests {

        @Test
        @DisplayName("TC1: Send OTP email successfully - covers all lines")
        void sendOtp_Success() {
            // Arrange
            String toEmail = "test@example.com";
            String otp = "123456";

            // Act
            emailService.sendOtp(toEmail, otp);

            // Assert
            ArgumentCaptor<SimpleMailMessage> messageCaptor = 
                    ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, times(1)).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getTo()).containsExactly(toEmail);
            assertThat(sentMessage.getSubject()).isEqualTo("Email Verification OTP");
            assertThat(sentMessage.getText()).isEqualTo("Your OTP is: " + otp);
        }

        @Test
        @DisplayName("TC2: Send OTP with different email and OTP values")
        void sendOtp_DifferentValues() {
            // Arrange
            String toEmail = "another@example.com";
            String otp = "789012";

            // Act
            emailService.sendOtp(toEmail, otp);

            // Assert
            ArgumentCaptor<SimpleMailMessage> messageCaptor = 
                    ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, times(1)).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getTo()).containsExactly(toEmail);
            assertThat(sentMessage.getSubject()).isEqualTo("Email Verification OTP");
            assertThat(sentMessage.getText()).isEqualTo("Your OTP is: " + otp);
        }

        @Test
        @DisplayName("TC3: Send OTP with empty string values")
        void sendOtp_EmptyValues() {
            // Arrange
            String toEmail = "";
            String otp = "";

            // Act
            emailService.sendOtp(toEmail, otp);

            // Assert
            ArgumentCaptor<SimpleMailMessage> messageCaptor = 
                    ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, times(1)).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getTo()).containsExactly(toEmail);
            assertThat(sentMessage.getText()).isEqualTo("Your OTP is: " + otp);
        }

        @Test
        @DisplayName("TC4: Send OTP with null values (should handle gracefully)")
        void sendOtp_NullValues() {
            // Arrange
            String toEmail = null;
            String otp = null;

            // Act & Assert - Should not throw exception
            // Note: In real scenario, mailSender might throw, but we're testing the service method
            assertThatCode(() -> emailService.sendOtp(toEmail, otp))
                    .doesNotThrowAnyException();

            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("TC5: Verify multiple calls to sendOtp")
        void sendOtp_MultipleCalls() {
            // Arrange
            String toEmail1 = "user1@example.com";
            String otp1 = "111111";
            String toEmail2 = "user2@example.com";
            String otp2 = "222222";

            // Act
            emailService.sendOtp(toEmail1, otp1);
            emailService.sendOtp(toEmail2, otp2);

            // Assert
            verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
        }
    }
}