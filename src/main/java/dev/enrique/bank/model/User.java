package dev.enrique.bank.model;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import dev.enrique.bank.commons.enums.UserRole;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "Invalid password")
    private String password;

    @Column(name = "email")
    @Email(regexp = ".+@.+\\..+", message = "Invalid email")
    private String email;

    @Column(name = "country")
    private String country;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "activation_code")
    private String activationCode;

    @Column(name = "gender")
    private String gender;

    @Column(name = "phone_number")
    private Long phoneNumber;

    @Column(name = "birthday")
    private String birthday;

    @Column(name = "phone_code")
    private String phoneCode;

    @Column(name = "active", nullable = false, columnDefinition = "boolean default false")
    private boolean active = false;

    @CreationTimestamp
    @Column(name = "registration_date")
    private LocalDateTime registrationDate;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Account> accounts;
}
