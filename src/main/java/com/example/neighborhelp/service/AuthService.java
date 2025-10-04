package com.example.neighborhelp.service;

import com.example.neighborhelp.dto.LoginRequest;
import com.example.neighborhelp.dto.UpdatedRegisterRequest;
import com.example.neighborhelp.dto.UserResponse;
import com.example.neighborhelp.entity.PasswordResetToken;
import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.entity.RefreshToken;
import com.example.neighborhelp.exception.InvalidTokenException;
import com.example.neighborhelp.exception.ResourceNotFoundException;
import com.example.neighborhelp.repository.UserRepository;
import com.example.neighborhelp.repository.RefreshTokenRepository;
import com.example.neighborhelp.repository.PasswordResetTokenRepository;
import com.example.neighborhelp.security.EmailService;
import com.example.neighborhelp.service.FirebaseService;
import com.example.neighborhelp.security.JwtService;
import com.example.neighborhelp.security.TokenResponse;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final FirebaseService firebaseService;

    public AuthService(UserRepository userRepository,PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authenticationManager,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository, EmailService emailService,
                       FirebaseService firebaseService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.firebaseService = firebaseService;
    }

    @Transactional
    public UserResponse register(UpdatedRegisterRequest request) throws FirebaseAuthException {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceNotFoundException("Email Already Exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceNotFoundException("Username Already Exists");
        }
        if (request.getType() == User.UserType.ORGANIZATION &&
                (request.getOrganizationName() == null || request.getOrganizationName().trim().isEmpty())) {
            throw new ResourceNotFoundException("Organizations must have an organization name");
        }

        // 1. Crear usuario en Firebase
        UserRecord firebaseUser ;
        try {
            firebaseUser  = firebaseService.createFirebaseUser(request.getEmail(), request.getPassword());
        } catch (Exception e) {
            throw new RuntimeException("Firebase user creation failed: " + e.getMessage());
        }

        // 2. Generar link de verificación Firebase con retry
        String verificationLink = FirebaseAuth.getInstance().generateEmailVerificationLink(firebaseUser.getEmail());

        // 3. Guardar usuario local sin password (o con password cifrada si quieres)
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirebaseUid(firebaseUser.getUid());
        user.setRole(request.getRole());
        user.setType(request.getType());
        user.setOrganizationName(request.getOrganizationName());
        user.setDescription(request.getDescription());
        user.setWebsite(request.getWebsite());
        user.setVerified(false);
        user.setEnabled(false);
        User savedUser  = userRepository.save(user);

        // 4. Enviar email de verificación agregando documento en Firestore para trigger
        if (verificationLink != null) {
            Firestore db = FirestoreClient.getFirestore();
            Map<String, Object> emailDoc = new HashMap<>();
            emailDoc.put("to", request.getEmail());
            emailDoc.put("subject", "Verify your NeighborHelp account");
            emailDoc.put("html", emailService.buildVerificationEmailHtml(verificationLink));
            emailDoc.put("from", "hello@neighborlyunion.com");
            emailDoc.put("status", "pending");
            db.collection("emails").add(emailDoc);
        }

        return mapToUserResponse(savedUser);
    }


    private String generateVerificationToken(User user) {
        // You can use UUID or any other method to generate a unique token
        return UUID.randomUUID().toString();
    }

    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = (User ) authentication.getPrincipal();

        // Consultar Firebase para verificar emailVerified
        UserRecord firebaseUser ;
        try {
            firebaseUser  = firebaseService.getFirebaseUser (request.getEmail());
        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Firebase user not found: " + e.getMessage());
        }

        if (!firebaseUser .isEmailVerified()) {
            throw new RuntimeException("Email not verified. Please check your email for verification.");
        }

        // Sincronizar estado local si es necesario
        if (!user.getVerified()) {
            user.setVerified(true);
            user.setEnabled(true);
            userRepository.save(user);
        }

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = createRefreshToken(user.getId());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .build();
    }



    private RefreshToken createRefreshToken(Long userId){
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(userRepository.findById(userId).get());
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public TokenResponse refreshToken(String token) throws InvalidTokenException {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));
        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Refresh token expired. Please log in again");
        }

        User user = refreshToken.getUser();
        String accessToken = jwtService.generateToken(user);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .build();
    }

    public void initiatePasswordReset(String email) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new ResourceNotFoundException("User not found with email: " + email));
        String token = UUID.randomUUID().toString();

        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken(token);
        passwordResetToken.setUser(user);
        passwordResetToken.setExpiryDate(LocalDateTime.now().plusHours(1));

        passwordResetTokenRepository.save(passwordResetToken);

       // String resetUrl = "https://neighbothelp.com/reset-password?token= " + token;
       // emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) throws InvalidTokenException {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid token"));
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())){
            passwordResetTokenRepository.delete(resetToken);
        }

        User user = resetToken.getUser();
        user.setPassword((newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
    }

    private UserResponse mapToUserResponse(User user) {
        return new UserResponse.Builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .type(user.getType())
                .organizationName((user.getOrganizationName()))
                .description(user.getDescription())
                .website(user.getWebsite())
                .verified(user.getVerified())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
