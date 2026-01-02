package com.duebook.app.service;

import com.duebook.app.dto.ResendEmailPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ResendEmailService resendEmailService;
    private final TemplateEngine templateEngine;

    @Value("${otp.expiration.minutes:5}")
    private long otpExpirationMinutes;

    private static final String OTP_PREFIX = "duebook-otp:";
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate a 6-digit OTP
     */
    public String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Store OTP in Redis with expiration
     */
    public void storeOtp(String email, String otp) {
        String key = OTP_PREFIX + email;
        redisTemplate.opsForValue().set(key, otp, otpExpirationMinutes, TimeUnit.MINUTES);
        log.info("OTP stored for email: {} with expiration of {} minutes", email, otpExpirationMinutes);
    }

    /**
     * Verify OTP
     */
    public boolean verifyOtp(String email, String otp) {
        String key = OTP_PREFIX + email;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp != null && storedOtp.equals(otp)) {
            // Delete OTP after successful verification
            redisTemplate.delete(key);
            log.info("OTP verified successfully for email: {}", email);
            return true;
        }

        log.warn("OTP verification failed for email: {}", email);
        return false;
    }

    /**
     * Send OTP via email (for login)
     */
    public void sendOtpEmail(String email, String name, String otp) {
        try {
            // Prepare Thymeleaf context
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("otp", otp);
            context.setVariable("expirationMinutes", otpExpirationMinutes);

            // Process the template
            String htmlContent = templateEngine.process("email/login-otp", context);

            // Create email payload
            ResendEmailPayload emailPayload = new ResendEmailPayload();
            emailPayload.setTo(new String[]{email});
            emailPayload.setSubject("Your Duebook Login Code");
            emailPayload.setHtml(htmlContent);

            // Send email
            resendEmailService.sendEmail(emailPayload);

            log.info("OTP email sent to: {}", email);
        } catch (Exception e) {
            log.error("Error sending OTP email to: {}", email, e);
            throw new RuntimeException("Failed to send OTP email");
        }
    }

    /**
     * Send OTP for adding secondary email
     */
    public void sendSecondaryEmailOtp(String email, String name, String otp) {
        try {
            // Prepare Thymeleaf context
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("otp", otp);

            // Process the template
            String htmlContent = templateEngine.process("email/add-secondary-email-otp", context);

            // Create email payload
            ResendEmailPayload emailPayload = new ResendEmailPayload();
            emailPayload.setTo(new String[]{email});
            emailPayload.setSubject("Verify Your Secondary Email - Duebook");
            emailPayload.setHtml(htmlContent);

            // Send email
            resendEmailService.sendEmail(emailPayload);

            log.info("Secondary email OTP sent to: {}", email);
        } catch (Exception e) {
            log.error("Error sending secondary email OTP to: {}", email, e);
            throw new RuntimeException("Failed to send secondary email OTP");
        }
    }

    /**
     * Send OTP for updating primary email
     */
    public void sendPrimaryEmailUpdateOtp(String email, String name, String otp) {
        try {
            // Prepare Thymeleaf context
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("otp", otp);

            // Process the template
            String htmlContent = templateEngine.process("email/update-primary-email-otp", context);

            // Create email payload
            ResendEmailPayload emailPayload = new ResendEmailPayload();
            emailPayload.setTo(new String[]{email});
            emailPayload.setSubject("Verify Your New Primary Email - Duebook");
            emailPayload.setHtml(htmlContent);

            // Send email
            resendEmailService.sendEmail(emailPayload);

            log.info("Primary email update OTP sent to: {}", email);
        } catch (Exception e) {
            log.error("Error sending primary email update OTP to: {}", email, e);
            throw new RuntimeException("Failed to send primary email update OTP");
        }
    }

    /**
     * Check if OTP exists for email
     */
    public boolean hasValidOtp(String email) {
        String key = OTP_PREFIX + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}

