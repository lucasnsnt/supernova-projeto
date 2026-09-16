package ink.lucasnsnt.supernovaprojeto.controllers;

import ink.lucasnsnt.supernovaprojeto.dtos.account.AccountResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.account.AccountProfileUpdateRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.account.EmailUpdateRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.account.PasswordUpdateRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.account.PhoneUpdateRequest;
import ink.lucasnsnt.supernovaprojeto.services.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public AccountResponse getAccount(@AuthenticationPrincipal Jwt jwt) {
        return accountService.findOwnAccount(userId(jwt));
    }

    @PatchMapping("/phone")
    public AccountResponse updatePhone(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PhoneUpdateRequest request) {
        return accountService.updatePhone(userId(jwt), request.phone());
    }

    @PatchMapping("/profile")
    public AccountResponse updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AccountProfileUpdateRequest request) {
        return accountService.updateOwnProfile(userId(jwt), request.name(), request.phone());
    }

    @PatchMapping("/email")
    public AccountResponse updateEmail(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EmailUpdateRequest request) {
        return accountService.updateEmail(userId(jwt), request.email(), request.registrationToken());
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PasswordUpdateRequest request) {
        accountService.updatePassword(userId(jwt), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
