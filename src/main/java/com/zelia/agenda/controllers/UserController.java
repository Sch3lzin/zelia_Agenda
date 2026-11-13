package com.zelia.agenda.controllers;

import com.zelia.agenda.domain.user.User;
import com.zelia.agenda.domain.user.dto.*;
import com.zelia.agenda.infra.ratelimit.RateLimiter;
import com.zelia.agenda.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Duration FORGOT_WINDOW = Duration.ofHours(1);

    private final UserService userService;
    private final RateLimiter rateLimiter;

    public UserController(UserService userService, RateLimiter rateLimiter) {
        this.userService = userService;
        this.rateLimiter = rateLimiter;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers()
                .stream()
                .map(UserResponse::from)
                .toList();

        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasRole('ADMIN') or #id == principal")
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
    public ResponseEntity<Void> forgotMyPassword(@RequestBody @Valid ForgotPasswordDTO dto,
                                                 HttpServletRequest request) {
        rateLimiter.check("forgot:ip:" + request.getRemoteAddr(), 10, FORGOT_WINDOW);
        rateLimiter.check("forgot:email:" + dto.email().trim().toLowerCase(Locale.ROOT), 3, FORGOT_WINDOW);

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
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id,
                                                       @AuthenticationPrincipal Long actorId) {
        User user = userService.deactivateUser(id, actorId);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
