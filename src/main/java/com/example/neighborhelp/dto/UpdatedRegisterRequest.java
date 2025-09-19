// Package declaration for the DTO (Data Transfer Object) class
package com.example.neighborhelp.dto;

// Importing necessary classes and annotations
import com.example.neighborhelp.entity.User.Role; // Importing the Role enum from the User entity
import com.example.neighborhelp.entity.User.UserType; // Importing the UserType enum from the User entity
import javax.validation.constraints.Email; // Importing the Email validation annotation
import javax.validation.constraints.NotBlank; // Importing the NotBlank validation annotation
import javax.validation.constraints.NotNull; // Importing the NotNull validation annotation
import javax.validation.constraints.Size; // Importing the Size validation annotation
import lombok.Data; // Importing Lombok's Data annotation for automatic getter/setter generation
import org.hibernate.validator.constraints.URL; // Importing the URL validation annotation

// Lombok's @Data annotation generates getters, setters, toString, equals, and hashCode methods
@Data
public class UpdatedRegisterRequest {

    // Field for username with validation to ensure it is not blank
    @NotBlank(message = "Username is required")
    private String username;

    // Field for email with validation to ensure it is not blank and follows email format
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    // Field for password with validation to ensure it is not blank and has a minimum length
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "The password must be at least 8 characters long.")
    private String password;

    // Field for role with a default value of USER
    private Role role = Role.USER;

    // Field for user type with validation to ensure it is not null
    @NotNull(message = "The user type is mandatory")
    private UserType type = UserType.INDIVIDUAL;

    // Field for organization name (optional)
    private String organizationName;

    // Field for a description (optional)
    private String description;

    // Field for website with URL validation
    @URL
    private String website;

    public @NotBlank(message = "Username is required") String getUsername() {return username;}
    public @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String getEmail() {return email;}
    public String getOrganizationName() {return organizationName;}
    public String getDescription() {return description;}
    public String getPassword() {return password;}
    public UserType getType() {return type;}
    public String getWebsite() {return website;}
    public Role getRole() {return role;}

}