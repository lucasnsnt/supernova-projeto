package ink.lucasnsnt.supernovaprojeto.controllers;

import ink.lucasnsnt.supernovaprojeto.dtos.account.AccountResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.driver.DriverProfileUpdateRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.driver.DriverResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.invite.InviteCreateRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.invite.InviteResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.link.LinkedStudentResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.vehicle.VehicleRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.vehicle.VehicleResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.route.RecurringRouteCreateRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.route.RecurringRouteResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.route.RouteEnrollmentResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.trip.DailyConfirmationResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.trip.DepartureUpdateRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.trip.TripCancellationRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.trip.TripResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.trip.TripVehicleUpdateRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.trip.TripLocationUpdateRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.trip.TripTrackingResponse;
import ink.lucasnsnt.supernovaprojeto.models.DriverInvite;
import ink.lucasnsnt.supernovaprojeto.models.Vehicle;
import ink.lucasnsnt.supernovaprojeto.services.AccountService;
import ink.lucasnsnt.supernovaprojeto.services.DriverInviteService;
import ink.lucasnsnt.supernovaprojeto.services.DriverService;
import ink.lucasnsnt.supernovaprojeto.services.DriverStudentLinkService;
import ink.lucasnsnt.supernovaprojeto.services.VehicleService;
import ink.lucasnsnt.supernovaprojeto.services.DailyConfirmationService;
import ink.lucasnsnt.supernovaprojeto.services.TripService;
import ink.lucasnsnt.supernovaprojeto.services.TripPlanningService;
import ink.lucasnsnt.supernovaprojeto.services.TripTrackingService;
import ink.lucasnsnt.supernovaprojeto.services.RecurringRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;

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
    private final DailyConfirmationService confirmationService;
    private final TripService tripService;
    private final TripPlanningService tripPlanningService;
    private final RecurringRouteService recurringRouteService;
    private final TripTrackingService tripTrackingService;
    private final ink.lucasnsnt.supernovaprojeto.services.RouteOperationService routeOperationService;

    @GetMapping("/routes/{routeId}/trip-preview")
    public ink.lucasnsnt.supernovaprojeto.dtos.route.OperationalRoutePreview operationalPreview(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long routeId, @RequestParam LocalDate date,
            @RequestParam ink.lucasnsnt.supernovaprojeto.models.enums.Direction direction) {
        return routeOperationService.preview(userId(jwt), routeId, date, direction);
    }

    @PostMapping("/routes/{routeId}/start")
    public TripResponse startRoute(@AuthenticationPrincipal Jwt jwt, @PathVariable Long routeId,
            @Valid @RequestBody ink.lucasnsnt.supernovaprojeto.dtos.route.RouteStartRequest request) {
        return routeOperationService.start(userId(jwt), routeId, request);
    }

    @DeleteMapping("/route-enrollments/{enrollmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRouteEnrollment(@AuthenticationPrincipal Jwt jwt, @PathVariable Long enrollmentId) {
        recurringRouteService.remove(userId(jwt), enrollmentId);
    }

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

    @PutMapping("/operational-address")
    public DriverResponse setOperationalAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddressRequest request) {
        return driverService.setOperationalAddress(userId(jwt), request);
    }

    @DeleteMapping("/operational-address")
    public DriverResponse useRegistrationAddressForOperation(
            @AuthenticationPrincipal Jwt jwt) {
        return driverService.useRegistrationAddressForOperation(userId(jwt));
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

    @GetMapping("/routes")
    public List<RecurringRouteResponse> findRecurringRoutes(@AuthenticationPrincipal Jwt jwt) {
        return recurringRouteService.findByDriver(userId(jwt));
    }

    @GetMapping("/route-previews")
    public List<ink.lucasnsnt.supernovaprojeto.dtos.route.RecurringRoutePreviewResponse> findRoutePreviews(
            @AuthenticationPrincipal Jwt jwt, @RequestParam LocalDate date) {
        return recurringRouteService.findPreviewsForDriver(userId(jwt), date);
    }

    @PostMapping("/routes")
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringRouteResponse createRecurringRoute(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RecurringRouteCreateRequest request) {
        return recurringRouteService.create(userId(jwt), request);
    }

    @GetMapping("/routes/{routeId}/enrollments")
    public List<RouteEnrollmentResponse> findRouteEnrollments(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long routeId) {
        return recurringRouteService.findEnrollmentsForDriver(userId(jwt), routeId);
    }

    @GetMapping("/daily-confirmations")
    public List<DailyConfirmationResponse> findDailyConfirmations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate date) {
        driverService.requireOperationalView(userId(jwt));
        return confirmationService.findByDriver(userId(jwt), date);
    }

    @GetMapping("/trips")
    public List<TripResponse> findTrips(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate date) {
        return tripService.findByDriver(userId(jwt), date);
    }

    @PutMapping("/trips/{tripId}/departure")
    public TripResponse updateTripDeparture(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tripId,
            @Valid @RequestBody DepartureUpdateRequest request) {
        return tripService.updateDeparture(userId(jwt), tripId, request.departureAt(), request.reason());
    }

    @PutMapping("/trips/{tripId}/vehicle")
    public TripResponse changeTripVehicle(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tripId,
            @Valid @RequestBody TripVehicleUpdateRequest request) {
        return tripService.changeVehicle(userId(jwt), tripId, request.vehicleId());
    }

    @PostMapping("/trips/{tripId}/replanning")
    public TripResponse replanTrip(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tripId) {
        return tripPlanningService.replan(userId(jwt), tripId);
    }

    @PostMapping("/trips/{tripId}/start")
    public TripResponse startTrip(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tripId,
            @RequestBody(required = false) ink.lucasnsnt.supernovaprojeto.dtos.trip.TripStartRequest request) {
        return routeOperationService.startTrip(userId(jwt), tripId, request != null && request.acknowledgeOutsideWindow());
    }

    @PostMapping("/trips/{tripId}/completion")
    public TripResponse completeTrip(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tripId) {
        return tripService.complete(userId(jwt), tripId);
    }

    @PostMapping("/trips/{tripId}/stops/completion")
    public TripResponse completeTripStop(@AuthenticationPrincipal Jwt jwt, @PathVariable Long tripId) {
        return tripService.completeNextStop(userId(jwt), tripId);
    }

    @PutMapping("/trips/{tripId}/location")
    public TripTrackingResponse updateTripLocation(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tripId, @Valid @RequestBody TripLocationUpdateRequest request) {
        return tripTrackingService.updateLocation(userId(jwt), tripId, request);
    }

    @GetMapping("/trips/{tripId}/tracking")
    public TripTrackingResponse trackTrip(@AuthenticationPrincipal Jwt jwt, @PathVariable Long tripId) {
        return tripTrackingService.findForDriver(userId(jwt), tripId);
    }

    @PostMapping("/trips/{tripId}/cancellation")
    public TripResponse cancelTrip(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long tripId,
            @Valid @RequestBody TripCancellationRequest request) {
        return tripService.cancel(userId(jwt), tripId, request.reason());
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

    @PutMapping("/vehicles/{vehicleId}/default")
    public VehicleResponse setDefaultVehicle(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long vehicleId) {
        return VehicleResponse.from(vehicleService.setDefault(userId(jwt), vehicleId));
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
