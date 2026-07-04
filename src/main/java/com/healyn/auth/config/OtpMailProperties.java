package com.healyn.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.Locale;

/** Sender identity and subject for OTP emails. SMTP transport is configured via {@code spring.mail.*}. */
@ConfigurationProperties(prefix = "healyn.otp.email")
@ConstructorBinding
public record OtpMailProperties(String from, String subject, String provider, String apiKey,
                                String apiBaseUrl) {

    public OtpMailProperties(String from, String subject) {
        this(from, subject, "smtp", "", "");
    }

    public OtpMailProperties {
        if (provider == null || provider.isBlank()) {
            provider = "smtp";
        }
        provider = provider.trim().toLowerCase(Locale.ROOT);
        if (apiBaseUrl == null) {
            apiBaseUrl = "";
        }
        if (apiKey == null) {
            apiKey = "";
        }
    }

    public boolean usesHttpApi() {
        return "resend".equals(provider) || "brevo".equals(provider);
    }

    public String resolvedApiBaseUrl() {
        if (apiBaseUrl != null && !apiBaseUrl.isBlank()) {
            return apiBaseUrl;
        }
        return "resend".equals(provider) ? "https://api.resend.com" : "https://api.brevo.com";
    }
}
