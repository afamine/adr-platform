package com.adrplatform.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailService {

    /**
     * Placeholder email sender. Replace with integration (SMTP, SES, etc.) when credentials are available.
     */
    public void sendPlainText(String from, String to, String subject, String body) {
        log.info("Simulated email from {} to {} with subject '{}'", from, to, subject);
        log.debug("Email body:\n{}", body);
    }
}
