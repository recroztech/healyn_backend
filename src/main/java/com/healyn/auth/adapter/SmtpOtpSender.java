package com.healyn.auth.adapter;

import com.healyn.auth.config.OtpMailProperties;
import com.healyn.auth.domain.OtpChannel;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Real EMAIL-channel OTP delivery over SMTP or a transactional email API (Resend/Brevo).
 * Active outside local/test, where {@link LoggingOtpSender} prints to the console instead.
 */
@Component
@Profile("!local & !test")
public class SmtpOtpSender implements ChannelOtpSender {

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;
    private final OtpMailProperties props;

    SmtpOtpSender(ObjectProvider<JavaMailSender> mailSender, OtpMailProperties props) {
        this(mailSender, new RestTemplateBuilder(), props);
    }

    @Autowired
    SmtpOtpSender(ObjectProvider<JavaMailSender> mailSender, RestTemplateBuilder restTemplateBuilder,
                 OtpMailProperties props) {
        this.mailSender = mailSender.getIfAvailable();
        this.restTemplate = restTemplateBuilder.build();
        this.props = props;
    }

    @PostConstruct
    void verifyConfigured() {
        if (props.getFrom() == null || props.getFrom().isBlank()) {
            throw new IllegalStateException("healyn.otp.email.from must be set for OTP email delivery.");
        }
        if (props.usesHttpApi()) {
            if (props.getApiKey() == null || props.getApiKey().isBlank()) {
                throw new IllegalStateException(
                        "OTP email delivery via " + props.getProvider() + " requires healyn.otp.email.api-key.");
            }
            return;
        }
        if (mailSender == null) {
            throw new IllegalStateException(
                    "OTP email delivery requires SMTP configuration (set HEALYN_SMTP_HOST and credentials) "
                            + "or a Resend/Brevo API provider configuration.");
        }
    }

    @Override
    public OtpChannel channel() {
        return OtpChannel.EMAIL;
    }

    @Override
    public void send(String target, String code) {
        String text = "Your Healyn verification code is " + code
                + ".\n\nIt expires in 5 minutes. If you did not request this, you can ignore this email.";

        if (props.usesHttpApi()) {
            sendViaHttpApi(target, text);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(props.getFrom());
        message.setTo(target);
        message.setSubject(props.getSubject());
        message.setText(text);
        // Never log the code (Hard Rule #3).
        mailSender.send(message);
    }

    private void sendViaHttpApi(String target, String text) {
        String endpoint = props.resolvedApiBaseUrl();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if ("resend".equals(props.getProvider())) {
            headers.setBearerAuth(props.getApiKey());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("from", props.getFrom());
            body.put("to", List.of(target));
            body.put("subject", props.getSubject());
            body.put("text", text);
            restTemplate.postForEntity(endpoint + "/emails", new HttpEntity<>(body, headers), String.class);
            return;
        }

        headers.set("api-key", props.getApiKey());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sender", Map.of("name", "Healyn", "email", props.getFrom()));
        body.put("to", List.of(Map.of("email", target)));
        body.put("subject", props.getSubject());
        body.put("text", text);
        restTemplate.postForEntity(endpoint + "/v3/smtp/email", new HttpEntity<>(body, headers), String.class);
    }
}
