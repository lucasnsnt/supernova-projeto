package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceConflictException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceNotFoundException;
import ink.lucasnsnt.supernovaprojeto.models.Driver;
import ink.lucasnsnt.supernovaprojeto.models.DriverInvite;
import ink.lucasnsnt.supernovaprojeto.models.enums.InviteStatus;
import ink.lucasnsnt.supernovaprojeto.repositories.DriverInviteRepository;
import ink.lucasnsnt.supernovaprojeto.dtos.invite.InvitePreviewResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.invite.InviteResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class DriverInviteService {

    public static final int DEFAULT_VALIDITY_DAYS = 7;

    private final DriverInviteRepository inviteRepository;
    private final DriverService driverService;
    private final Clock clock;

    @Transactional
    public DriverInvite create(@NotNull Long driverId, boolean replaceCurrent) {
        return create(driverId, DEFAULT_VALIDITY_DAYS, replaceCurrent);
    }

    @Transactional
    public DriverInvite create(
            @NotNull Long driverId,
            @Positive int validityDays,
            boolean replaceCurrent) {
        Driver driver = driverService.requireApproved(driverId);
        DriverInvite current = inviteRepository
                .findFirstByDriverIdAndStatus(driverId, InviteStatus.ACTIVE)
                .orElse(null);

        if (current != null && isExpired(current)) {
            markExpired(current);
            current = null;
        }
        if (current != null && !replaceCurrent) {
            throw new ResourceConflictException(
                    "O motorista já possui um convite ativo; confirme a substituição para criar outro");
        }
        if (current != null) {
            markRevoked(current);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        DriverInvite invite = DriverInvite.builder()
                .driver(driver)
                .token(UUID.randomUUID().toString())
                .createdAt(now)
                .expiresAt(now.plusDays(validityDays))
                .status(InviteStatus.ACTIVE)
                .build();
        driver.addInvite(invite);
        return inviteRepository.save(invite);
    }

    @Transactional
    public DriverInvite findUsableByToken(@NotBlank String token) {
        DriverInvite invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Convite", token));
        if (invite.getStatus() == InviteStatus.ACTIVE && isExpired(invite)) {
            markExpired(invite);
        }
        if (invite.getStatus() != InviteStatus.ACTIVE) {
            throw new BusinessRuleException("O convite não está ativo");
        }
        driverService.requireApproved(invite.getDriver().getId());
        return invite;
    }

    @Transactional
    public DriverInvite revoke(@NotNull Long driverId, @NotNull Long inviteId) {
        driverService.requireApproved(driverId);
        DriverInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Convite", inviteId));
        if (!invite.getDriver().getId().equals(driverId)) {
            throw new ResourceNotFoundException("Convite", inviteId);
        }
        if (invite.getStatus() != InviteStatus.ACTIVE) {
            throw new BusinessRuleException("Somente um convite ativo pode ser revogado");
        }
        markRevoked(invite);
        return invite;
    }

    @Transactional
    public InvitePreviewResponse preview(@NotBlank String token) {
        DriverInvite invite = findUsableByToken(token);
        return new InvitePreviewResponse(invite.getDriver().getId(),
                invite.getDriver().getUser().getName(), invite.getExpiresAt());
    }

    @Transactional
    public List<InviteResponse> findAllByDriver(@NotNull Long driverId) {
        driverService.requireOperationalView(driverId);
        List<DriverInvite> invites = inviteRepository.findAllByDriverId(driverId);
        invites.stream()
                .filter(invite -> invite.getStatus() == InviteStatus.ACTIVE && isExpired(invite))
                .forEach(this::markExpired);
        return invites.stream()
                .map(InviteResponse::from)
                .toList();
    }

    private boolean isExpired(DriverInvite invite) {
        return !invite.getExpiresAt().isAfter(LocalDateTime.now(clock));
    }

    private void markExpired(DriverInvite invite) {
        invite.setStatus(InviteStatus.EXPIRED);
        invite.setExpiredAt(LocalDateTime.now(clock));
    }

    private void markRevoked(DriverInvite invite) {
        invite.setStatus(InviteStatus.REVOKED);
        invite.setRevokedAt(LocalDateTime.now(clock));
    }
}
