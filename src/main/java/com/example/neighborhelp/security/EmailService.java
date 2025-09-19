package com.example.neighborhelp.security;

import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String apiKey;

    @Value("${app.api.url:https://api.neighborlyunion.com}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String RESEND_URL = "https://api.resend.com/emails";

    public void sendVerificationEmail(String toEmail, String verificationToken) {
        String verificationUrl = generateVerificationUrl(verificationToken);
        String subject = "Verify your NeighborHelp account";
        String html = buildVerificationEmailHtml(verificationUrl);

        sendEmail(toEmail, subject, html);
    }

    public String generateVerificationUrl(String token) {
        try {
            return new URIBuilder()
                    .setScheme("https")
                    .setHost("api.neighborlyunion.com")  // Tu Railway API
                    .setPath("/api/v1/auth/verify")
                    .setParameter("token", token)
                    .build()
                    .toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error generating verification URL", e);
        }
    }

    private String buildVerificationEmailHtml(String verificationUrl) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #2E86C1;">Welcome to NeighborHelp!</h2>
                <p>Please click the button below to verify your account:</p>
                
                <div style="text-align: center; margin: 25px 0;">
                    <a href="%s" 
                       style="display: inline-block; padding: 12px 24px; 
                       background-color: #2E86C1; color: white; 
                       text-decoration: none; border-radius: 4px; font-weight: bold;">
                       Verify Account
                    </a>
                </div>
                
                <p>If you didn't create an account with us, please ignore this email.</p>
                <p><small>Link: %s</small></p>
            </div>
            """.formatted(verificationUrl, verificationUrl);
    }

    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        String subject = "Password reset Request";
        String html = "<h2>Password Reset</h2>" +
                "<p>Click the link below to reset your password:</p>" +
                "<a href=\"" + resetUrl + "\">Reset Password</a>";

        sendEmail(toEmail, subject, html);
    }

    private void sendEmail(String toEmail, String subject, String htmlContent) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> request = new HashMap<>();
        request.put("from", "NeighborHelp <hello@neighborlyunion.com>");
        request.put("to", toEmail);
        request.put("subject", subject);
        request.put("html", htmlContent);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(RESEND_URL, HttpMethod.POST, entity, String.class);
            System.out.println("Email sent: " + response.getStatusCode());
            System.out.println(response.getBody());
        } catch (Exception e){
            System.err.println("Email sending failed: " + e.getMessage());
        }
    }
}