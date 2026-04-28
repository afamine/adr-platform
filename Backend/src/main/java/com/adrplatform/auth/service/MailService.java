package com.adrplatform.auth.service;

import com.adrplatform.auth.config.PasswordResetProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
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

    /**
     * Sends the email-verification message asynchronously so registration is not blocked.
     * Loads the HTML template from classpath, embeds the brand logo inline
     * via cid, and uses the existing Brevo SMTP configuration.
     */
    @org.springframework.scheduling.annotation.Async
    public void sendVerificationEmail(String toEmail, String fullName, String verificationUrl, int expiryHours) {
        String template = loadTemplate("templates/email-verification.html");
        String html = template
                .replace("{{fullName}}", escapeHtml(fullName == null ? "" : fullName))
                .replace("{{link}}", verificationUrl)
                .replace("{{expiryHours}}", Integer.toString(expiryHours));

        Map<String, String> inlines = new LinkedHashMap<>();
        inlines.put("logo_header", "assets/logos/png/logo-horizontal-dark-accent.png");

        sendHtmlWithInlines(
                passwordResetProperties.getEmailFrom(),
                toEmail,
                "Verify your ADR Manager account",
                html,
                inlines
        );
    }

    private String loadTemplate(String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream is = cl.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Template not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load email template", e);
        }
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public void sendHtmlWithInlines(String from, String to, String subject, String htmlBody,
                                    Map<String, String> inlineClasspathByCid) {
        try {
            log.debug("Attempting to send HTML email with inlines to {} with subject '{}'", to, subject);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_RELATED,
                    "UTF-8"
            );
            helper.setFrom(from != null ? from : passwordResetProperties.getEmailFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            if (inlineClasspathByCid != null) {
                for (Map.Entry<String, String> e : inlineClasspathByCid.entrySet()) {
                    String cid = e.getKey();
                    String path = e.getValue();
                    String lower = path.toLowerCase();
                    String contentType;
                    if (lower.endsWith(".png")) contentType = "image/png";
                    else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) contentType = "image/jpeg";
                    else if (lower.endsWith(".gif")) contentType = "image/gif";
                    else {
                        log.warn("Skipping inline cid='{}' path='{}': unsupported type for Gmail (only PNG/JPEG/GIF)", cid, path);
                        continue;
                    }
                    ClassPathResource res = new ClassPathResource(path);
                    if (!res.exists()) {
                        log.warn("Skipping inline cid='{}': classpath resource not found: {}", cid, path);
                        continue;
                    }
                    helper.addInline(cid, res, contentType);
                }
            }

            // Force Content-Disposition: inline on every related body part so Gmail does not list them as attachments
            forceInlineDispositionOnRelatedParts(message);

            mailSender.send(message);
            log.info("HTML email with inlines sent to {} with subject '{}'", to, subject);
        } catch (Exception ex) {
            log.error("Failed to send HTML email with inlines to {}", to, ex);
            throw new IllegalStateException("Unable to send email", ex);
        }
    }

    private void forceInlineDispositionOnRelatedParts(MimeMessage message) throws MessagingException {
        Object content;
        try {
            content = message.getContent();
        } catch (Exception ex) {
            return;
        }
        if (!(content instanceof Multipart)) return;
        walkAndForceInline((Multipart) content);
    }

    private void walkAndForceInline(Multipart mp) throws MessagingException {
        for (int i = 0; i < mp.getCount(); i++) {
            var part = mp.getBodyPart(i);
            try {
                Object inner = part.getContent();
                if (inner instanceof Multipart) {
                    walkAndForceInline((Multipart) inner);
                    continue;
                }
            } catch (Exception ignored) {}
            String[] cids = part.getHeader("Content-ID");
            if (cids != null && cids.length > 0 && part instanceof MimeBodyPart) {
                part.setDisposition(Part.INLINE);
            }
        }
    }
}
