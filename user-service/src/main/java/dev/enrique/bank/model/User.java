package dev.enrique.bank.model;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Gender;
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

    @Column(name = "email", nullable = false, unique = true)
    @Email(message = "Invalid email")
    private String email;

    @Column(name = "phone_code", nullable = false)
    private String phoneCode;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "names", nullable = false)
    private String names;

    @Column(name = "first_surname", nullable = false)
    private String firstSurname;

    @Column(name = "second_surname", nullable = false)
    private String secondSurname;

    @Column(name = "gender", nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "birthday", nullable = false)
    private String birthday;

    @Column(name = "curp", nullable = false)
    private String curp;

    @Column(name = "rfc", nullable = false)
    private String rfc;

    @Column(name = "country_of_birth", length = 2, nullable = false)
    @Enumerated(EnumType.STRING)
    private Country countryOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "active", nullable = false, columnDefinition = "boolean default false")
    private boolean active = false;

    @Column(name = "password_reset_code")
    private String passwordResetCode;

    @Column(name = "activation_code")
    private String activationCode;

    @CreationTimestamp
    @Column(name = "registration_date")
    private LocalDateTime registrationDate;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    //@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    //private List<Notification> notifications;
}
