package com.zelia.agenda.domain.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);
    // Senha padrão de versões anteriores. Se ainda estiver ativa, a conta deve ser trocada.
    private static final String LEGACY_DEFAULT_PASSWORD = "admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        var existing = userRepository.findByEmailIgnoreCase(adminEmail);

        if (existing.isPresent()) {
            if (passwordEncoder.matches(LEGACY_DEFAULT_PASSWORD, existing.get().getPassword())) {
                log.error("A conta administradora ainda usa a senha padrão. Troque a senha imediatamente.");
            }
            return;
        }

        PasswordPolicy.validate(adminPassword);

        User admin = new User();
        admin.setName("Administrador");
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setUserType(UserType.ADMIN);
        admin.setActive(true);

        userRepository.save(admin);
    }
}
