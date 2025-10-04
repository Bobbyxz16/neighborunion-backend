package com.example.neighborhelp.service;

import com.example.neighborhelp.dto.UserResponse;
import com.example.neighborhelp.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    /**
     * Mapea una entidad User a un DTO UserResponse usando el Builder.
     * @param user La entidad User a mapear
     * @return UserResponse con los campos mapeados (null si user es null)
     */
    public UserResponse mapToUserResponse(User user) {
        if (user == null) {
            return null;
        }

        // Usa el Builder para mapear campo por campo
        return new UserResponse.Builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole()) // Enum Role de User
                .type(user.getType()) // Enum UserType de User
                .organizationName(user.getOrganizationName())
                .description(user.getDescription())
                .website(user.getWebsite())
                .verified(user.getVerified())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();

        // Nota: Si algún campo es opcional o puede ser null, el Builder lo maneja automáticamente.
        // Si quieres valores por defecto (e.g., verified = false si null), agrega lógica:
        // .verified(user.getVerified() != null ? user.getVerified() : false)
    }

    // Opcional: Para mapear una lista de usuarios
    public List<UserResponse> mapToUserResponseList(List<User> users) {
        if (users == null) {
            return List.of();
        }
        return users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }
}
