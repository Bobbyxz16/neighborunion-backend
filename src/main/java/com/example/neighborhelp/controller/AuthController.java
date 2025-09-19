package com.example.neighborhelp.controller;

import com.example.neighborhelp.dto.*;
import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.repository.UserRepository;
import com.example.neighborhelp.exception.InvalidTokenException;
import com.example.neighborhelp.exception.ResourceNotFoundException;
import com.example.neighborhelp.security.TokenResponse;
import com.example.neighborhelp.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import javax.validation.Valid;

@RestController
@RequestMapping("api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UpdatedRegisterRequest request){
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
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request){
        authService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok(new MessageResponse("An email with instructions has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) throws InvalidTokenException {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(new MessageResponse("Password reset successful"));
    }
}