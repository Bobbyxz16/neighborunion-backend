package com.example.neighborhelp.repository;


import com.example.neighborhelp.entity.RefreshToken;
import com.example.neighborhelp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
}
