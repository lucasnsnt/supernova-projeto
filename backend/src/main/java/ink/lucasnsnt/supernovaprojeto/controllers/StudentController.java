package ink.lucasnsnt.supernovaprojeto.controllers;

import ink.lucasnsnt.supernovaprojeto.dtos.account.AccountResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.institution.InstitutionResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.link.StudentLinkResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.student.InstitutionSelectionRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.student.InviteAcceptanceRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.student.ScheduleResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.student.ScheduleUpdateRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.student.StudentProfileStatus;
import ink.lucasnsnt.supernovaprojeto.dtos.student.StudentProfileUpdateRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.student.StudentDetailsResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.trip.DailyConfirmationAnswerRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.trip.DailyConfirmationResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.trip.TripResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.trip.TripTrackingResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.route.RecurringRouteResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.route.RouteEnrollmentRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.route.RouteEnrollmentResponse;
import ink.lucasnsnt.supernovaprojeto.services.AccountService;
import ink.lucasnsnt.supernovaprojeto.services.DriverStudentLinkService;
import ink.lucasnsnt.supernovaprojeto.services.StudentScheduleService;
import ink.lucasnsnt.supernovaprojeto.services.StudentService;
import ink.lucasnsnt.supernovaprojeto.services.DailyConfirmationService;
import ink.lucasnsnt.supernovaprojeto.services.TripService;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/students/me")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentController {

    private final AccountService accountService;
    private final StudentService studentService;
    private final StudentScheduleService scheduleService;
    private final DriverStudentLinkService linkService;
    private final DailyConfirmationService confirmationService;
    private final TripService tripService;
    private final RecurringRouteService recurringRouteService;
    private final TripTrackingService tripTrackingService;

    @GetMapping
    public StudentDetailsResponse getDetails(@AuthenticationPrincipal Jwt jwt) {
        return studentService.getDetails(userId(jwt));
    }

    @PutMapping("/profile")
    public AccountResponse updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody StudentProfileUpdateRequest request) {
        return accountService.updateStudentProfile(userId(jwt), request);
    }

    @GetMapping("/profile-status")
    public StudentProfileStatus getProfileStatus(@AuthenticationPrincipal Jwt jwt) {
        return studentService.getProfileStatus(userId(jwt));
    }

    @PutMapping("/institution")
    public InstitutionResponse selectInstitution(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InstitutionSelectionRequest request) {
        return studentService.selectInstitutionResponse(userId(jwt), request.institutionId());
    }

    @GetMapping("/schedules")
    public List<ScheduleResponse> findSchedules(@AuthenticationPrincipal Jwt jwt) {
        return scheduleService.findAllByStudent(userId(jwt)).stream()
                .map(schedule -> new ScheduleResponse(schedule.getId(), schedule.getDayOfWeek(),
                        schedule.getTime(), schedule.getDirection()))
                .toList();
    }

    @PutMapping("/schedules/{dayOfWeek}")
    public List<ScheduleResponse> setSchedule(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable DayOfWeek dayOfWeek,
            @Valid @RequestBody ScheduleUpdateRequest request) {
        return scheduleService.setForDay(
                        userId(jwt), dayOfWeek, request.outboundTime(), request.returnTime()).stream()
                .map(schedule -> new ScheduleResponse(schedule.getId(), schedule.getDayOfWeek(),
                        schedule.getTime(), schedule.getDirection()))
                .toList();
    }

    @DeleteMapping("/schedules/{dayOfWeek}")
    public ResponseEntity<Void> removeSchedule(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable DayOfWeek dayOfWeek) {
        scheduleService.removeDay(userId(jwt), dayOfWeek);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/links")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentLinkResponse acceptInvite(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InviteAcceptanceRequest request) {
        return linkService.acceptInviteResponse(userId(jwt), request.token());
    }

    @GetMapping("/links")
    public List<StudentLinkResponse> findLinkHistory(@AuthenticationPrincipal Jwt jwt) {
        return linkService.findHistoryByStudent(userId(jwt));
    }

    @DeleteMapping("/links/{linkId}")
    public StudentLinkResponse endLink(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long linkId) {
        return linkService.endByStudentResponse(userId(jwt), linkId);
    }

    @GetMapping("/available-routes")
    public List<RecurringRouteResponse> findAvailableRoutes(@AuthenticationPrincipal Jwt jwt) {
        return recurringRouteService.findAvailableForStudent(userId(jwt));
    }

    @GetMapping("/route-enrollments")
    public List<RouteEnrollmentResponse> findRouteEnrollments(@AuthenticationPrincipal Jwt jwt) {
        return recurringRouteService.findEnrollmentsForStudent(userId(jwt));
    }

    @GetMapping("/route-previews")
    public List<ink.lucasnsnt.supernovaprojeto.dtos.route.RecurringRoutePreviewResponse> findRoutePreviews(
            @AuthenticationPrincipal Jwt jwt, @RequestParam LocalDate date) {
        return recurringRouteService.findPreviewsForStudent(userId(jwt), date);
    }

    @PostMapping("/route-enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public RouteEnrollmentResponse requestRouteEnrollment(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RouteEnrollmentRequest request) {
        return recurringRouteService.requestEnrollment(userId(jwt), request);
    }

    @GetMapping("/daily-confirmations")
    public List<DailyConfirmationResponse> findDailyConfirmations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate date) {
        return confirmationService.findByStudent(userId(jwt), date);
    }

    @PutMapping("/daily-confirmations/{confirmationId}/answer")
    public DailyConfirmationResponse answerDailyConfirmation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long confirmationId,
            @Valid @RequestBody DailyConfirmationAnswerRequest request) {
        return confirmationService.answer(userId(jwt), confirmationId, request.answer());
    }

    @GetMapping("/trips")
    public List<TripResponse> findTrips(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate date) {
        return tripService.findByStudent(userId(jwt), date);
    }

    @GetMapping("/trips/{tripId}/tracking")
    public TripTrackingResponse trackTrip(@AuthenticationPrincipal Jwt jwt, @PathVariable Long tripId) {
        return tripTrackingService.findForStudent(userId(jwt), tripId);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
