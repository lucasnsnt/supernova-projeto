package ink.lucasnsnt.supernovaprojeto.controllers;

import ink.lucasnsnt.supernovaprojeto.dtos.auth.*;
import ink.lucasnsnt.supernovaprojeto.security.IssuedTokens;
import ink.lucasnsnt.supernovaprojeto.services.AuthCookieService;
import ink.lucasnsnt.supernovaprojeto.services.AuthService;
import ink.lucasnsnt.supernovaprojeto.services.EmailVerificationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmailVerificationService emailVerificationService;
    private final AuthService authService;
    private final AuthCookieService cookieService;

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/email-verification")
    public ResponseEntity<Void> requestEmailVerification(
            @Valid @RequestBody EmailVerificationRequest request) {
        emailVerificationService.requestCode(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/email-verification/confirm")
    public RegistrationAuthorizationResponse confirmEmail(
            @Valid @RequestBody EmailCodeConfirmationRequest request) {
        return emailVerificationService.confirmCode(request.email(), request.code());
    }

    @PostMapping("/register")
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        return respondWithSession(authService.register(request), response);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        return respondWithSession(authService.login(request.email(), request.password()), response);
    }

    @PostMapping("/refresh")
    public AuthRefreshResponse refresh(
            @CookieValue(name = "${app.security.refresh-cookie-name}", required = false) String refreshToken,
            HttpServletResponse response) {
        IssuedTokens tokens = authService.refresh(refreshToken);
        cookieService.writeRefreshToken(response, tokens.refreshToken());
        return new AuthRefreshResponse(
                tokens.accessToken(), "Bearer", tokens.accessTokenExpiresAt());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${app.security.refresh-cookie-name}", required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        cookieService.clearRefreshToken(response);
        return ResponseEntity.noContent().build();
    }

    private AuthResponse respondWithSession(IssuedTokens tokens, HttpServletResponse response) {
        cookieService.writeRefreshToken(response, tokens.refreshToken());
        return authService.toResponse(tokens);
    }
}
