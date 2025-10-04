package com.example.neighborhelp.controller;

import com.example.neighborhelp.dto.*;
import com.example.neighborhelp.entity.RefreshToken;
import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.repository.UserRepository;
import com.example.neighborhelp.exception.InvalidTokenException;
import com.example.neighborhelp.exception.ResourceNotFoundException;
import com.example.neighborhelp.security.TokenResponse;
import com.example.neighborhelp.service.AuthService;
import com.example.neighborhelp.service.FirebaseService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.example.neighborhelp.service.UserService;
import com.example.neighborhelp.security.JwtService;
import com.example.neighborhelp.service.RefreshTokenService;
import com.google.firebase.auth.UserRecord;
import com.example.neighborhelp.security.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import javax.validation.Valid;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final FirebaseService  firebaseService;
    private final EmailService emailService;


    public AuthController(AuthService authService, UserRepository userRepository,
                          JwtService jwtService, RefreshTokenService refreshTokenService,
                          UserService userService, FirebaseService firebaseService, EmailService emailService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
        this.firebaseService = firebaseService;
        this.emailService = emailService;
    }

    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UpdatedRegisterRequest request) throws FirebaseAuthException {
        UserResponse userResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request){
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/verify")
    public RedirectView verifyUser(@RequestParam String token) {
        try {
            User user = userRepository.findByVerificationToken(token)
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid verification token"));

            // Set user as enabled and verified
            user.setEnabled(true);
            user.setVerified(true);
            user.setVerificationToken(null); // Clear the token after successful verification
            userRepository.save(user);

            // Redirect to your Vercel frontend
            return new RedirectView("https://neighborlyunion.com/verify-success");

        } catch (ResourceNotFoundException e) {
            // Redirect to an error page if token is invalid
            return new RedirectView("https://neighborlyunion.com/verify-error?error=invalid-token");
        } catch (Exception e) {
            // Redirect to an error page for any other exceptions
            return new RedirectView("https://neighborlyunion.com/verify-error?error=verification-failed");
        }
    }

    // Alternative endpoint that returns JSON response (useful for API testing)
    @GetMapping("/verify-status")
    public ResponseEntity<MessageResponse> verifyUserStatus(@RequestParam String token) {
        try {
            User user = userRepository.findByVerificationToken(token)
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid verification token"));

            user.setEnabled(true);
            user.setVerified(true);
            user.setVerificationToken(null);
            userRepository.save(user);

            return ResponseEntity.ok(new MessageResponse("Account verified successfully. You can now log in."));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Invalid verification token"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Verification failed. Please try again."));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) throws InvalidTokenException {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) throws IOException {
        authService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok(new MessageResponse("An email with instructions has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) throws InvalidTokenException {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(new MessageResponse("Password reset successful"));
    }



    @PostMapping("/firebase-register")
    public ResponseEntity<Map<String, Object>> firebaseRegister(@Valid @RequestBody UpdatedRegisterRequest request) {
        try {
            // 1. Create Firebase user (without email verification)
            UserRecord firebaseUser = firebaseService.createFirebaseUser(request.getEmail(), request.getPassword());

            // 2. Generate YOUR OWN verification token
            String verificationToken = UUID.randomUUID().toString();

            // 3. Save user to YOUR database with the token
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setFirebaseUid(firebaseUser.getUid());
            user.setAuthProvider("FIREBASE");
            user.setRole(request.getRole());
            user.setType(request.getType());
            user.setOrganizationName(request.getOrganizationName());
            user.setDescription(request.getDescription());
            user.setWebsite(request.getWebsite());
            user.setVerificationToken(verificationToken); // Your token
            user.setEnabled(false);
            user.setVerified(false);
            User savedUser = userRepository.save(user);

            // 4. Create YOUR verification link (points to your API)
            String verificationLink = "https://api.neighborlyunion.com/api/auth/verify-email?token=" + verificationToken;

            // 5. Send email via Firestore Trigger
            Firestore db = FirestoreClient.getFirestore();
            Map<String, Object> emailDoc = new HashMap<>();
            emailDoc.put("to", request.getEmail());

            Map<String, Object> message = new HashMap<>();
            message.put("subject", "Verify your NeighborHelp account");
            message.put("html", emailService.buildVerificationEmailHtml(verificationLink));

            emailDoc.put("message", message);

            ApiFuture<DocumentReference> addedDocRef = db.collection("mail").add(emailDoc);
            DocumentReference docRef = addedDocRef.get();
            System.out.println("Email document added: " + docRef.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully. Check email for verification.");
            response.put("userId", savedUser.getId());
            response.put("email", savedUser.getEmail());
            response.put("firebaseUid", firebaseUser.getUid());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Verify email endpoint - handles your custom verification token
     */
    @GetMapping("/verify-email")
    public RedirectView verifyEmail(@RequestParam String token) {
        try {
            // 1. Find user by your verification token
            User user = userRepository.findByVerificationToken(token)
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired verification token"));

            // 2. Update YOUR database
            user.setEnabled(true);
            user.setVerified(true);
            user.setVerificationToken(null); // Clear token after use
            userRepository.save(user);

            // 3. Also mark as verified in Firebase
            UserRecord.UpdateRequest updateRequest = new UserRecord.UpdateRequest(user.getFirebaseUid())
                    .setEmailVerified(true);
            FirebaseAuth.getInstance().updateUser(updateRequest);

            System.out.println("User verified: " + user.getEmail());

            // 4. Redirect to success page
            return new RedirectView("https://neighborlyunion.com/verify-success");

        } catch (ResourceNotFoundException e) {
            System.err.println("Verification failed: " + e.getMessage());
            return new RedirectView("https://neighborlyunion.com/verify-error?error=invalid-token");
        } catch (FirebaseAuthException e) {
            System.err.println("Firebase update failed: " + e.getMessage());
            // Still redirect to success since database was updated
            return new RedirectView("https://neighborlyunion.com/verify-success");
        } catch (Exception e) {
            e.printStackTrace();
            return new RedirectView("https://neighborlyunion.com/verify-error?error=verification-failed");
        }
    }

    /**
     * Login with Firebase
     */
    @PostMapping("/firebase-login")
    public ResponseEntity<Map<String, Object>> firebaseLogin(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            // 1. Get Firebase user
            UserRecord firebaseUser = firebaseService.getFirebaseUser(email);

            // 2. Find user in YOUR database
            User user = userRepository.findByFirebaseUid(firebaseUser.getUid())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // 3. Check if user is verified in YOUR system
            if (!user.getVerified() || !user.getEnabled()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Email not verified. Please check your email and verify your account.");
                error.put("verified", false);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            // 4. Generate tokens
            String customToken = firebaseService.createCustomToken(firebaseUser.getUid());
            String accessToken = jwtService.generateToken(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("firebaseCustomToken", customToken);
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken.getToken());
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtService.getExpirationTime());
            response.put("user", userService.mapToUserResponse(user));

            return ResponseEntity.ok(response);

        } catch (FirebaseAuthException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Firebase authentication failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (ResourceNotFoundException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Login failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Check verification status
     */
    @PostMapping("/check-verification")
    public ResponseEntity<Map<String, Object>> checkVerification(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("email", user.getEmail());
            response.put("verified", user.getVerified());
            response.put("enabled", user.getEnabled());
            response.put("authProvider", user.getAuthProvider());

            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User not found: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}