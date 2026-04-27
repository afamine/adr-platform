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
     * Reuses the existing Brevo SMTP configuration via {@link #sendHtml}.
     */
    @org.springframework.scheduling.annotation.Async
    public void sendVerificationEmail(String toEmail, String fullName, String verificationUrl) {
        String safeName = fullName == null ? "" : fullName;
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#f4f4f7;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" role="presentation" style="background:#f4f4f7;padding:24px 0;">
                    <tr><td align="center">
                      <table width="100%%" cellpadding="0" cellspacing="0" role="presentation" style="max-width:600px;background:#ffffff;border-radius:8px;overflow:hidden;">
                        <tr><td style="background:#111827;padding:18px 24px;color:#ffffff;font-size:18px;font-weight:700;">ADR Manager</td></tr>
                        <tr><td style="padding:32px;">
                          <h1 style="margin:0 0 12px 0;color:#111827;font-size:22px;">Verify your email address</h1>
                          <p style="color:#4b5563;font-size:15px;line-height:1.6;margin:0 0 20px 0;">Hello %s,</p>
                          <p style="color:#4b5563;font-size:15px;line-height:1.6;margin:0 0 24px 0;">
                            Thank you for creating your ADR Manager account. Please verify your email address by clicking the button below.
                          </p>
                          <table width="100%%" cellpadding="0" cellspacing="0" role="presentation" style="margin-bottom:20px;">
                            <tr><td align="center">
                              <a href="%s" style="background:#020617;color:#ffffff;text-decoration:none;padding:14px 36px;border-radius:6px;font-size:16px;font-weight:bold;display:inline-block;">
                                Verify Email Address
                              </a>
                            </td></tr>
                          </table>
                          <p style="color:#9ca3af;font-size:13px;text-align:center;margin:0;">
                            This link expires in 24 hours. If you didn't create this account, you can safely ignore this email.
                          </p>
                        </td></tr>
                        <tr><td style="border-top:1px solid #e5e7eb;padding:18px 32px;text-align:center;color:#9ca3af;font-size:12px;">
                          &copy; 2026 ADR Manager &middot; Final Year Project
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(escapeHtml(safeName), verificationUrl);
        sendHtml(null, toEmail, "Verify your ADR Manager account", html);
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
