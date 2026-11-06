package com.zelia.agenda.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType userType;

    @Column(nullable = false)
    private Boolean active;

    public User(@NotBlank String name, @NotBlank String email, @Nullable String encode, @NotNull UserType userType, @Nullable Boolean active) {
        this.name = name;
        this.email = email;
        this.password = encode;
        this.userType = userType;
        this.active = active;
    }
}