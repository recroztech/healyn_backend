package com.healyn.auth.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

/** Sender identity and subject for OTP emails. SMTP transport is configured via {@code spring.mail.*}. */
@ConfigurationProperties(prefix = "healyn.otp.email")
public class OtpMailProperties {

    private String from;
    private String subject = "Your Healyn verification code";
    private String provider = "smtp";
    private String apiKey = "";
    private String apiBaseUrl = "";

    public OtpMailProperties() {
    }

    public OtpMailProperties(String from, String subject) {
        this.from = from;
        this.subject = subject;
    }

    public OtpMailProperties(String from, String subject, String provider, String apiKey, String apiBaseUrl) {
        this.from = from;
        this.subject = subject;
        this.provider = provider;
        this.apiKey = apiKey;
        this.apiBaseUrl = apiBaseUrl;
    }

    @PostConstruct
    public void initialize() {
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

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
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
