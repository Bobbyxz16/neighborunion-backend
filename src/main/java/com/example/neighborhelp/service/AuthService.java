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
import com.example.neighborhelp.security.JwtService;
import com.example.neighborhelp.security.TokenResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
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

    public AuthService(UserRepository userRepository,PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authenticationManager,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository, EmailService emailService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
    }

    @Transactional
    public UserResponse register(UpdatedRegisterRequest request){
        if (userRepository.existsByEmail((request.getEmail()))){
            throw new ResourceNotFoundException("Email Already Exists");
        }

        if (userRepository.existsByUsername(request.getUsername())){
            throw new ResourceNotFoundException("Username Already Exists");
        }

        if (request.getType() == User.UserType.ORGANIZATION &&
                (request.getOrganizationName() == null || request.getOrganizationName().trim().isEmpty())){
            throw new ResourceNotFoundException("Organizations must have an organization name");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setType(request.getType());
        user.setOrganizationName(request.getOrganizationName());
        user.setDescription(request.getDescription());
        user.setWebsite(request.getWebsite());
        user.setVerified(false);
        user.setEnabled(false);
        String verificationToken = generateVerificationToken(user);
        user.setVerificationToken(verificationToken);
        User savedUser = userRepository.save(user);

        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);

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
        if (!user.getEnabled()) {
            String verificationToken = generateVerificationToken(user);
            emailService.sendVerificationEmail(user.getEmail(), verificationToken);

            throw new RuntimeException(request.getEmail() + " is not enabled, please check your email for verification.");
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

    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new ResourceNotFoundException("User not found with email: " + email));
        String token = UUID.randomUUID().toString();

        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken(token);
        passwordResetToken.setUser(user);
        passwordResetToken.setExpiryDate(LocalDateTime.now().plusHours(1));

        passwordResetTokenRepository.save(passwordResetToken);

        String resetUrl = "https://neighbothelp.com/reset-password?token= " + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
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
