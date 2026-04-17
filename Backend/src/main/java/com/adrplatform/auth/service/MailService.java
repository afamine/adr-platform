package com.adrplatform.auth.service;

import com.adrplatform.auth.config.PasswordResetProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final PasswordResetProperties passwordResetProperties;

    public void sendPlainText(String from, String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from != null ? from : passwordResetProperties.getEmailFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.info("Email sent to {} with subject '{}'", to, subject);
        } catch (Exception ex) {
            log.error("Failed to send email to {}", to, ex);
            throw new IllegalStateException("Unable to send email", ex);
        }
    }
}
