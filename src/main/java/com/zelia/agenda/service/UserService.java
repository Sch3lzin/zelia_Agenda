package com.zelia.agenda.service;

import com.zelia.agenda.domain.user.User;
import com.zelia.agenda.domain.user.UserRepository;
import com.zelia.agenda.domain.user.dto.CreateUserDTO;
import com.zelia.agenda.exceptions.RegisteredEmailException;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(@Valid CreateUserDTO dto) {
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new RegisteredEmailException(dto.email());
        }

        User user = new User(dto.name(),
                dto.email(),
                passwordEncoder.encode(dto.password()),
                dto.userType(),
                true);

        return userRepository.save(user);
    }
}
