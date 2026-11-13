package com.zelia.agenda.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

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

    private String token;

    @Column(name = "expiration_token")
    private LocalDateTime expirationToken;

    @Column(nullable = false)
    private Boolean active;

    // Incrementado para invalidar todos os tokens JWT já emitidos para este usuário.
    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    public User(@NotBlank String name, @NotBlank String email, @Nullable String encode, @NotNull UserType userType, @Nullable Boolean active) {
        this.name = name;
        this.email = email;
        this.password = encode;
        this.userType = userType;
        this.active = active;
    }

    public User update(@NotBlank String name, @NotBlank String email, @NotNull UserType userType) {
        this.name = name;
        this.email = email;
        this.userType = userType;
        return this;
    }

    public void invalidateTokens() {
        this.tokenVersion++;
    }
}
