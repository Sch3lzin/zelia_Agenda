package com.zelia.agenda.controllers;

import com.zelia.agenda.domain.user.User;
import com.zelia.agenda.domain.user.dto.*;
import com.zelia.agenda.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        User user = userService.getUser(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody @Valid CreateUserDTO dto) {
        User user = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/forgot-my-password")
    public ResponseEntity<Void> forgotMyPassword(@RequestBody @Valid ForgotPasswordDTO dto) {
        userService.sendToken(dto);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/recovery-account")
    public ResponseEntity<Void> recoveryAccount(@RequestBody @Valid ResetPasswordDTO dto) {
        userService.resetPassword(dto);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody @Valid UpdateUserDTO dto) {
        User user = userService.updateUser(id, dto);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/reactive/{id}")
    public ResponseEntity<UserResponse> reactiveUser(@PathVariable Long id) {
        User user = userService.reactiveUser(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        User user = userService.deactivateUser(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
