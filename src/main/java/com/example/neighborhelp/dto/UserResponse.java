package com.example.neighborhelp.dto;

import com.example.neighborhelp.entity.User.Role; // Importing the Role enum from the User entity
import com.example.neighborhelp.entity.User.UserType; // Importing the UserType enum from the User entity
import java.time.LocalDateTime;

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private UserType type;
    private String organizationName;
    private String description;
    private String website;
    private Boolean verified;
    private Boolean enabled;
    private LocalDateTime createdAt;

    // Private constructor to enforce the use of the Builder
    private UserResponse(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.email = builder.email;
        this.role = builder.role;
        this.type = builder.type;
        this.organizationName = builder.organizationName;
        this.description = builder.description;
        this.website = builder.website;
        this.verified = builder.verified;
        this.enabled = builder.enabled;
        this.createdAt = builder.createdAt;
    }

    // Public getters for each field
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public UserType getType() {
        return type;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getDescription() {
        return description;
    }

    public String getWebsite() {
        return website;
    }

    public Boolean getVerified() {
        return verified;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Static inner Builder class
    public static class Builder {
        private Long id;
        private String username;
        private String email;
        private Role role;
        private UserType type;
        private String organizationName;
        private String description;
        private String website;
        private Boolean verified;
        private Boolean enabled;
        private LocalDateTime createdAt;

        // Builder methods for each field
        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder type(UserType type) {
            this.type = type;
            return this;
        }

        public Builder organizationName(String organizationName) {
            this.organizationName = organizationName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder website(String website) {
            this.website = website;
            return this;
        }

        public Builder verified(Boolean verified) {
            this.verified = verified;
            return this;
        }

        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        // Build method to create an instance of UserResponse
        public UserResponse build() {
            return new UserResponse(this);
        }
    }
}
