package com.example.neighborhelp.security;  // Ajusta según tu package

import org.springframework.beans.factory.annotation.Value;  // Para @Value
import org.springframework.http.HttpEntity;  // Para HttpEntity
import org.springframework.http.HttpHeaders;  // Para HttpHeaders
import org.springframework.http.HttpMethod;  // Para HttpMethod.POST
import org.springframework.http.MediaType;  // Para MediaType.APPLICATION_JSON
import org.springframework.http.ResponseEntity;  // Para ResponseEntity
import org.springframework.stereotype.Service;  // Para @Service
import org.springframework.web.client.RestTemplate;  // Para RestTemplate
import java.util.HashMap;  // Para HashMap
import java.util.Map;  // Para Map

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendVerificationEmail(String toEmail, String verificationLink) {
        String url = "https://api.resend.com/emails";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, Object> emailRequest = new HashMap<>();
        emailRequest.put("from", "NeighborHelp <no-reply@neighborlyunion.com>");
        emailRequest.put("to", toEmail);
        emailRequest.put("subject", "Verify your NeighborHelp account");
        emailRequest.put("html", buildVerificationEmailHtml(verificationLink));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(emailRequest, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            System.out.println("Resend API Response: " + response.getStatusCode());
            System.out.println("Response body: " + response.getBody());
        } catch (Exception e) {
            System.err.println("Resend email failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String buildVerificationEmailHtml(String verificationLink) {
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
                
                <p>Or copy this link: <a href="%s">%s</a></p>
                <p>If you didn't create an account with us, please ignore this email.</p>
            </div>
            """.formatted(verificationLink, verificationLink, verificationLink);
    }
}
