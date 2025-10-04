package com.example.neighborhelp.service;

import com.example.neighborhelp.entity.RefreshToken;
import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.repository.RefreshTokenRepository;
import com.example.neighborhelp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${app.jwt.refresh-token.expiry-days:7}")
    private int refreshTokenExpiryDays;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * Crea un nuevo refresh token para un usuario.
     * @param userId ID del usuario
     * @return El refresh token creado y persistido
     * @throws RuntimeException si el usuario no existe
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        // Opcional: elimina tokens anteriores si quieres single-session
        // refreshTokenRepository.deleteByUserId(userId);

        String token = generateToken();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));

        LocalDateTime expiryDate = LocalDateTime.now().plusDays(refreshTokenExpiryDays);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setExpiryDate(expiryDate);
        refreshToken.setUser(user);

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Genera un token único
     */
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Busca un refresh token por su valor
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Verifica si un token es válido (no expirado)
     */
    public boolean isTokenValid(String token) {
        return findByToken(token)
                .map(refreshToken -> !refreshToken.isExpired())
                .orElse(false);
    }

    /**
     * Invalida todos los refresh tokens de un usuario
     */
    @Transactional
    public void invalidateAllUserTokens(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    /**
     * Invalida un refresh token específico
     */
    @Transactional
    public void invalidateRefreshToken(String token) {
        findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    /**
     * Limpia tokens expirados manualmente
     */
    @Transactional
    public void cleanupExpiredTokens() {
        List<RefreshToken> allTokens = refreshTokenRepository.findAll();
        allTokens.stream()
                .filter(RefreshToken::isExpired)
                .forEach(refreshTokenRepository::delete);
    }
}
