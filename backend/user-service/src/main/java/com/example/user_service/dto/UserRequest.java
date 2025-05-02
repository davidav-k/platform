package com.example.user_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRequest {

    @NotEmpty(message = "First name cannot be empty or null")
    private String firstName;

    @NotEmpty(message = "Last name cannot be empty or null")
    private String lastName;

    @NotEmpty(message = "Email cannot be empty or null")
    @Email(message = "Invalid email address")
    private String email;

    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$",
            message = "Password must contain at least one digit, one lowercase letter, one uppercase letter, and be at least 8 characters long")

    @NotEmpty(message = "Password cannot be empty or null")
    private String password;

    private String bio;

    @Pattern(regexp = "^(\\+\\d{1,3}[- ]?)?\\d{10}$", message = "Invalid phone number")
    private String phone;
}

