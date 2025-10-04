package com.example.neighborhelp.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Service
public class FirebaseService {

    @Value("${firebase.config.path:neighborhelp-e7f2b-firebase-adminsdk-fbsvc-1c5604399a.json}")
    private String firebaseConfigPath;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = new ClassPathResource(firebaseConfigPath).getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();



                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }

    /**
     * Crear usuario en Firebase y enviar email de verificación automáticamente
     */
    public UserRecord createFirebaseUser(String email, String password) throws FirebaseAuthException {
        CreateRequest request = new CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setEmailVerified(false); // Firebase enviará email de verificación

        UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);

        // Firebase automáticamente enviará el email de verificación
        // cuando el usuario intente hacer login por primera vez

        return userRecord;
    }

    public void updateUserEmailVerified(String uid, boolean verified) throws FirebaseAuthException {
        UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
                .setEmailVerified(verified);
        FirebaseAuth.getInstance().updateUser (request);
        System.out.println("Firebase user " + uid + " email verified set to " + verified);
    }

    /**
     * Verificar si el usuario existe y está verificado
     */
    public UserRecord getFirebaseUser(String email) throws FirebaseAuthException {
        return FirebaseAuth.getInstance().getUserByEmail(email);
    }

    /**
     * Generar custom token para testing (simula el login de Firebase)
     */
    public String createCustomToken(String firebaseUid) throws FirebaseAuthException {
        return FirebaseAuth.getInstance().createCustomToken(firebaseUid);
    }

    /**
     * Verificar si el email está verificado
     */
    public boolean isEmailVerified(String email) {
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(email);
            return userRecord.isEmailVerified();
        } catch (FirebaseAuthException e) {
            return false;
        }
    }
}