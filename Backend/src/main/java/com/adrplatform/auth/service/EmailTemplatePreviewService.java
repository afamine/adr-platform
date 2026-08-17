package com.adrplatform.auth.service;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
public class EmailTemplatePreviewService {

    private static final Map<String, PreviewDefinition> PREVIEWS = Map.of(
            "email-verification", new PreviewDefinition("templates/email-verification.html", Map.of(
                    "fullName", "Alex Morgan",
                    "link", "https://app.axiom.example/verify-email?token=preview-token",
                    "expiryHours", "24"
            )),
            "password-reset", new PreviewDefinition("templates/password-reset-email.html", Map.of(
                    "fullName", "Alex Morgan",
                    "link", "https://app.axiom.example/reset-password?token=preview-token",
                    "minutes", "20"
            ))
    );

    public String renderPreview(String templateName) {
        PreviewDefinition definition = PREVIEWS.get(templateName);
        if (definition == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown email template: " + templateName);
        }

        ClassPathResource resource = new ClassPathResource(definition.resourcePath());
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            Mustache template = new DefaultMustacheFactory().compile(reader, definition.resourcePath());
            StringWriter rendered = new StringWriter();
            template.execute(rendered, definition.sampleData()).flush();
            return rendered.toString()
                    .replace("cid:logo_header", "assets/logos/png/logo-horizontal-dark-accent.png")
                    .replace("cid:brand_icon", "assets/logos/png/lock.png");
        } catch (Exception exception) {
            log.error("Unable to render email template preview {}", templateName, exception);
            throw new IllegalStateException("Unable to render email template preview", exception);
        }
    }

    private record PreviewDefinition(String resourcePath, Map<String, String> sampleData) {}
}
