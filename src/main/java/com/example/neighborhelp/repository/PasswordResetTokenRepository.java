package com.example.neighborhelp.repository;
import com.example.neighborhelp.entity.PasswordResetToken;
import com.example.neighborhelp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken>  findByToken(String token);
    //PasswordResetToken findByToken(String token);
    void deleteByUser(User user);
}
