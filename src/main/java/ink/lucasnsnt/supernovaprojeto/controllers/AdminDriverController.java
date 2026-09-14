package ink.lucasnsnt.supernovaprojeto.controllers;

import ink.lucasnsnt.supernovaprojeto.dtos.driver.DriverResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.driver.StatusReasonRequest;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStatus;
import ink.lucasnsnt.supernovaprojeto.services.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/drivers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDriverController {

    private final DriverService driverService;

    @GetMapping
    public List<DriverResponse> findAll(@RequestParam(required = false) DriverStatus status) {
        return driverService.findAll(status);
    }

    @GetMapping("/{driverId}")
    public DriverResponse getDetails(@PathVariable Long driverId) {
        return driverService.getDetails(driverId);
    }

    @PostMapping("/{driverId}/approval")
    public DriverResponse approve(@PathVariable Long driverId) {
        return driverService.approveResponse(driverId);
    }

    @PostMapping("/{driverId}/rejection")
    public DriverResponse reject(
            @PathVariable Long driverId,
            @Valid @RequestBody StatusReasonRequest request) {
        return driverService.rejectResponse(driverId, request.reason());
    }

    @PostMapping("/{driverId}/suspension")
    public DriverResponse suspend(
            @PathVariable Long driverId,
            @Valid @RequestBody StatusReasonRequest request) {
        return driverService.suspendResponse(driverId, request.reason());
    }

    @PostMapping("/{driverId}/reactivation")
    public DriverResponse reactivate(@PathVariable Long driverId) {
        return driverService.reactivateResponse(driverId);
    }
}
