package ink.lucasnsnt.supernovaprojeto.controllers;

import ink.lucasnsnt.supernovaprojeto.dtos.account.AccountResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.driver.DriverProfileUpdateRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.driver.DriverResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.invite.InviteCreateRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.invite.InviteResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.link.LinkedStudentResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.vehicle.VehicleRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.vehicle.VehicleResponse;
import ink.lucasnsnt.supernovaprojeto.models.DriverInvite;
import ink.lucasnsnt.supernovaprojeto.models.Vehicle;
import ink.lucasnsnt.supernovaprojeto.services.AccountService;
import ink.lucasnsnt.supernovaprojeto.services.DriverInviteService;
import ink.lucasnsnt.supernovaprojeto.services.DriverService;
import ink.lucasnsnt.supernovaprojeto.services.DriverStudentLinkService;
import ink.lucasnsnt.supernovaprojeto.services.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers/me")
@PreAuthorize("hasRole('DRIVER')")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;
    private final AccountService accountService;
    private final DriverInviteService inviteService;
    private final DriverStudentLinkService linkService;
    private final VehicleService vehicleService;

    @GetMapping
    public DriverResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return driverService.getDetails(userId(jwt));
    }

    @PutMapping("/profile")
    public AccountResponse updateRejectedProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DriverProfileUpdateRequest request) {
        return accountService.updateRejectedDriverProfile(userId(jwt), request);
    }

    @PostMapping("/review-submissions")
    public DriverResponse resubmitForReview(@AuthenticationPrincipal Jwt jwt) {
        return driverService.resubmitForReviewResponse(userId(jwt));
    }

    @GetMapping("/invites")
    public List<InviteResponse> findInvites(@AuthenticationPrincipal Jwt jwt) {
        return inviteService.findAllByDriver(userId(jwt));
    }

    @PostMapping("/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse createInvite(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InviteCreateRequest request) {
        int days = request.validityDays() == null
                ? DriverInviteService.DEFAULT_VALIDITY_DAYS
                : request.validityDays();
        DriverInvite invite = inviteService.create(userId(jwt), days, request.replaceCurrent());
        return InviteResponse.from(invite);
    }

    @DeleteMapping("/invites/{inviteId}")
    public InviteResponse revokeInvite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long inviteId) {
        return InviteResponse.from(inviteService.revoke(userId(jwt), inviteId));
    }

    @GetMapping("/students")
    public List<LinkedStudentResponse> findStudentHistory(@AuthenticationPrincipal Jwt jwt) {
        return linkService.findHistoryByDriver(userId(jwt));
    }

    @DeleteMapping("/students/{linkId}")
    public LinkedStudentResponse endStudentLink(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long linkId) {
        return linkService.endByDriverResponse(userId(jwt), linkId);
    }

    @GetMapping("/vehicles")
    public List<VehicleResponse> findVehicles(@AuthenticationPrincipal Jwt jwt) {
        return vehicleService.findAllByDriver(userId(jwt)).stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @PostMapping("/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse createVehicle(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VehicleRequest request) {
        Vehicle vehicle = vehicleService.create(userId(jwt), request.brand(), request.model(), request.year(),
                request.licensePlate(), request.passengerCapacity(), request.color());
        return VehicleResponse.from(vehicle);
    }

    @PutMapping("/vehicles/{vehicleId}")
    public VehicleResponse updateVehicle(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId,
            @Valid @RequestBody VehicleRequest request) {
        Vehicle vehicle = vehicleService.update(userId(jwt), vehicleId, request.brand(), request.model(),
                request.year(), request.licensePlate(), request.passengerCapacity(), request.color());
        return VehicleResponse.from(vehicle);
    }

    @DeleteMapping("/vehicles/{vehicleId}")
    public ResponseEntity<Void> deleteVehicle(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId) {
        vehicleService.delete(userId(jwt), vehicleId);
        return ResponseEntity.noContent().build();
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
