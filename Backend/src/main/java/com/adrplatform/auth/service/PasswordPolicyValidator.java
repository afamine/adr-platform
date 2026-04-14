package com.adrplatform.auth.service;

import com.adrplatform.auth.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordPolicyValidator {

    private static final Pattern PASSWORD_POLICY = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    public void validate(String rawPassword) {
        if (rawPassword == null || !PASSWORD_POLICY.matcher(rawPassword).matches()) {
            throw new BadRequestException("Password must be at least 8 chars and contain at least one letter and one number");
        }
    }
}
