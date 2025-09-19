package com.example.neighborhelp.entity;



import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Entity class representing a user in the system.
 * Maps to the 'users' table in the database.
 */
//@Data // Lombok: generates boilerplate code (getters, setters, etc.)
@Entity // Marks this class as a JPA entity (database table)
@Table(name = "users") // Specifies the table name in the database
public class User {

    /**
     * Primary key - auto-generated unique identifier
     */


    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment in database
    private Long id;

    /**
     * Unique username for login
     */
    @Column(nullable = false, unique = true) // Not null and unique constraint
    private String username;

    /**
     * Unique email address
     */
    @Column(nullable = false, unique = true) // Not null and unique constraint
    private String email;

    /**
     * Hashed password for security
     */
    @Column(nullable = false) // Not null
    private String password;

    /**
     * User role with default value of USER
     */
    @Enumerated(EnumType.STRING) // Store enum as string in database
    @Column(nullable = false) // Not null
    private Role role = Role.USER; // Default role

    /**
     * Type of user (individual or organization)
     */
    @Enumerated(EnumType.STRING) // Store enum as string in database
    @Column(nullable = false) // Not null
    @NotNull
    private UserType type;

    /**
     * Organization name (only applicable for organization users)
     */
    private String organizationName; // Optional field (can be null)

    /**
     * User description - uses TEXT type in database for longer content
     */
    @Column(columnDefinition = "TEXT") // Database column type definition
    private String description; // Optional field

    /**
     * Website URL
     */
    private String website; // Optional field

    /**
     * Account verification status - defaults to false
     */
    @Column(nullable = false) // Not null
    private Boolean verified = false; // Default value

    /**
     * Account enabled status - defaults to false
     */
    @Column(nullable = false) // Not null
    private Boolean enabled = false; // Default value

    /**
     * Automatic timestamp for creation date
     */
    @CreationTimestamp // Hibernate sets this automatically on creation
    @Column(name = "created_at", nullable = false, updatable = false) // Not null, can't be updated
    private LocalDateTime createdAt;

    /**
     * Automatic timestamp for last update date
     */
    @UpdateTimestamp // Hibernate updates this automatically on changes
    @Column(name = "updated_at", nullable = false) // Not null
    private LocalDateTime updatedAt;

    @Column(name = "verification_token")
    private String verificationToken;

    /**
     * Enum defining possible user roles
     */
    public enum Role {
        USER,      // Standard user permissions
        ADMIN,     // Full system access
        MODERATOR  // Limited administrative access
    }

    /**
     * Enum defining user types
     */
    public enum UserType {
        INDIVIDUAL,   // Represents a single person
        ORGANIZATION  // Represents a company/organization
    }

    public Long getId() { return id; }
    public String getUsername() {return username;}
    public Role getRole() {return role;}
    public String getEmail() {return email;}
    public String getPassword() {return password;}
    public Boolean getEnabled() {return enabled;}
    public UserType getType() {return type;}
    public String getOrganizationName() {return organizationName;}
    public String getDescription() {return description;}
    public String getWebsite() {return website;}
    public Boolean getVerified() {return verified;}
    public LocalDateTime getCreatedAt() {return createdAt;}
    public LocalDateTime getUpdatedAt() {return updatedAt;}
    public String getVerificationToken() {return verificationToken;}


    public void setId(Long id) { this.id = id;}
    public void setUsername(String username) { this.username = username;}
    public void setRole(Role role) { this.role = role;}
    public void setEmail(String email) {this.email = email;}
    public void setPassword(String password) {this.password = password;}
    public void setEnabled(Boolean enabled) {this.enabled = enabled;}
    public void setOrganizationName(String organizationName) {this.organizationName = organizationName;}
    public void setDescription(String description) {this.description = description;}
    public void setWebsite(String website) {this.website = website;}
    public void setVerified(Boolean verified) {this.verified = verified;}
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}
    public void setUpdatedAt(LocalDateTime updatedAt) {this.updatedAt = updatedAt;}
    public void setType(UserType type) {this.type = type;}
    public void setVerificationToken(String verificationToken) {this.verificationToken = verificationToken;}
}
