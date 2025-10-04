package com.example.neighborhelp.repository;

import com.example.neighborhelp.entity.RefreshToken;
import com.example.neighborhelp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // Opcional, pero recomendado

import java.util.List;
import java.util.Optional;

@Repository // Esto es estándar y no debería causar problemas
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Ya lo tienes: Busca por token
    Optional<RefreshToken> findByToken(String token);

    // Ya lo tienes: Elimina por usuario completo (Spring lo maneja)
    void deleteByUser (User user);

    // Nuevo: Busca todos los tokens de un usuario (derivado automático)
    List<RefreshToken> findByUser (User user);

    // Nuevo: Elimina por ID de usuario (derivado automático, si tu entidad tiene user.id accesible)
    void deleteByUserId(Long userId);

    // Nuevo: Busca tokens no expirados por usuario (esto es más avanzado; si no funciona sin @Query, omítelo y hazlo en el servicio)
    // List<RefreshToken> findByUser AndExpiryDateAfter(User user, LocalDateTime now);
    // Nota: Si este no compila sin @Query, ignóralo por ahora.
}
