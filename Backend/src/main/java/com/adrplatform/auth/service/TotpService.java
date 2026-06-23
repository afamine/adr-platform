package com.adrplatform.auth.service;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Slf4j
@Service
public class TotpService {

    private final dev.samstevens.totp.secret.SecretGenerator secretGen =
        new DefaultSecretGenerator(32);
    private final CodeVerifier codeVerifier;

    public TotpService() {
        CodeGenerator codeGen = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
        this.codeVerifier = new DefaultCodeVerifier(codeGen, new SystemTimeProvider());
        ((DefaultCodeVerifier) this.codeVerifier).setAllowedTimePeriodDiscrepancy(1);
    }

    public String generateSecret() {
        return secretGen.generate();
    }

    public String generateQrCodeBase64(String email, String secret) {
        try {
            QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer("ADR Platform")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
            byte[] imageBytes = new ZxingPngQrGenerator().generate(data);
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (QrGenerationException e) {
            log.error("QR code generation failed for {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != 6) {
            return false;
        }
        try {
            return codeVerifier.isValidCode(secret, code);
        } catch (Exception e) {
            log.error("TOTP verification error: {}", e.getMessage());
            return false;
        }
    }
}
