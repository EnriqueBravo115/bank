package dev.enrique.bank.model;

import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
public class UserProfile {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @Column(name = "names", nullable = false)
    private String names;

    @Column(name = "first_surname", nullable = false)
    private String firstSurname;

    @Column(name = "second_surname", nullable = false)
    private String secondSurname;

    @Column(name = "birthday", length = 10)
    private String birthday;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 50)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "country_of_birth", length = 2)
    private Country countryOfBirth;
}
