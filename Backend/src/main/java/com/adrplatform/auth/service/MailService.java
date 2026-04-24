package com.adrplatform.auth.service;

import com.adrplatform.auth.config.PasswordResetProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {


    private final JavaMailSender mailSender;
    private final PasswordResetProperties passwordResetProperties;

    public void sendPlainText(String from, String to, String subject, String body) {
        try {
            log.debug("Attempting to send email to {} with subject '{}'", to, subject);
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

    public void sendHtml(String from, String to, String subject, String htmlBody) {
        try {
            log.debug("Attempting to send HTML email to {} with subject '{}'", to, subject);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from != null ? from : passwordResetProperties.getEmailFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("HTML email sent to {} with subject '{}'", to, subject);
        } catch (Exception ex) {
            log.error("Failed to send HTML email to {}", to, ex);
            throw new IllegalStateException("Unable to send email", ex);
        }
    }

    public void sendHtmlWithInlines(String from, String to, String subject, String htmlBody, Map<String, String> inlineClasspathByCid) {
        try {
            log.debug("Attempting to send HTML email with inlines to {} with subject '{}'", to, subject);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from != null ? from : passwordResetProperties.getEmailFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (inlineClasspathByCid != null) {
                for (Map.Entry<String, String> e : inlineClasspathByCid.entrySet()) {
                    String path = e.getValue();
                    String lower = path.toLowerCase();
                    String contentType = lower.endsWith(".svg") ? "image/svg+xml"
                            : lower.endsWith(".png") ? "image/png"
                            : (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) ? "image/jpeg"
                            : lower.endsWith(".gif") ? "image/gif" : "application/octet-stream";
                    helper.addInline(e.getKey(), new ClassPathResource(path), contentType);
                }
            }
            mailSender.send(message);
            log.info("HTML email with inlines sent to {} with subject '{}'", to, subject);
        } catch (Exception ex) {
            log.error("Failed to send HTML email with inlines to {}", to, ex);
            throw new IllegalStateException("Unable to send email", ex);
        }
    }
}
