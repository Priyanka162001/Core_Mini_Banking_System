package in.bank.service;

import in.bank.entity.CustomerProfile;
import in.bank.repository.CustomerProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private JavaMailSender mailSender;

    // Generate OTP and save to DB
    public String generateAndSendOtp(Long customerProfileId) {
        CustomerProfile profile = customerProfileRepository.findById(customerProfileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        // Generate 6-digit OTP
        Random random = new Random();
        String otp = String.valueOf(100000 + random.nextInt(900000));

        profile.getCustomer().setOtp(otp);
        profile.getCustomer().setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        customerProfileRepository.save(profile);

        // Send OTP by email
        sendOtpEmail(profile.getCustomer().getEmail(), otp);

        return otp;
    }

    private void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your OTP Code");
        message.setText("Your OTP is: " + otp);
        message.setFrom("your-email@gmail.com");

        mailSender.send(message);
    }
}