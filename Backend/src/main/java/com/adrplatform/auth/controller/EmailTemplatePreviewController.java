package com.adrplatform.auth.controller;

import com.adrplatform.auth.service.EmailTemplatePreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/email-templates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmailTemplatePreviewController {

    private final EmailTemplatePreviewService emailTemplatePreviewService;

    @GetMapping(value = "/{templateName}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(@PathVariable String templateName) {
        return ResponseEntity.ok(emailTemplatePreviewService.renderPreview(templateName));
    }
}
