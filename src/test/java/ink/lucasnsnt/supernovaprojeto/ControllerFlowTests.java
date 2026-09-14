package ink.lucasnsnt.supernovaprojeto;

import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.institution.InstitutionRequest;
import ink.lucasnsnt.supernovaprojeto.models.Address;
import ink.lucasnsnt.supernovaprojeto.models.Driver;
import ink.lucasnsnt.supernovaprojeto.models.User;
import ink.lucasnsnt.supernovaprojeto.models.enums.InstitutionType;
import ink.lucasnsnt.supernovaprojeto.models.enums.NotificationType;
import ink.lucasnsnt.supernovaprojeto.models.enums.Role;
import ink.lucasnsnt.supernovaprojeto.repositories.DriverInviteRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.UserRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.VehicleRepository;
import ink.lucasnsnt.supernovaprojeto.services.DriverService;
import ink.lucasnsnt.supernovaprojeto.services.InstitutionService;
import ink.lucasnsnt.supernovaprojeto.services.InAppNotificationService;
import ink.lucasnsnt.supernovaprojeto.services.StudentScheduleService;
import ink.lucasnsnt.supernovaprojeto.services.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ControllerFlowTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private DriverInviteRepository inviteRepository;
    @Autowired private StudentService studentService;
    @Autowired private StudentScheduleService scheduleService;
    @Autowired private DriverService driverService;
    @Autowired private InstitutionService institutionService;
    @Autowired private InAppNotificationService notificationService;

    @Test
    void studentShouldConfigureOneWayScheduleUsingOnlyOwnJwtIdentity() throws Exception {
        User studentUser = saveUser("Aluno Controller", "student-controller@test.local", Role.STUDENT);
        studentService.register(studentUser.getId());
        Long institutionId = createInstitution("Instituição Controller");

        mockMvc.perform(put("/api/students/me/institution")
                        .with(jwtFor(studentUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"institutionId\":" + institutionId + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/students/me/schedules/MONDAY")
                        .with(jwtFor(studentUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outboundTime\":\"07:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].direction").value("IDA"));

        mockMvc.perform(get("/api/students/me/profile-status").with(jwtFor(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complete").value(true))
                .andExpect(jsonPath("$.hasOutboundSchedule").value(true))
                .andExpect(jsonPath("$.hasReturnSchedule").value(false));

        mockMvc.perform(get("/api/students/me/profile-status")
                        .with(jwt().jwt(token -> token.subject(studentUser.getId().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_DRIVER"))))
                .andExpect(status().isForbidden());

        var notification = notificationService.create(studentUser.getId(),
                NotificationType.DEPARTURE_REMINDER, "Horário da viagem",
                "Confira o horário previsto de saída", null, null);

        mockMvc.perform(get("/api/me/notifications/unread-count").with(jwtFor(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(patch("/api/me/notifications/{id}/read", notification.id())
                        .with(jwtFor(studentUser)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readAt").isNotEmpty());
    }

    @Test
    void shouldEnforceDriverApprovalAndSupportInviteWithoutTokenInUrl() throws Exception {
        User driverUser = saveUser("Motorista Controller", "driver-controller@test.local", Role.DRIVER);
        Driver driver = driverService.register(driverUser.getId(), "CNH-CONTROLLER");

        String vehicleBody = """
                {"brand":"Ford","model":"Ka","year":2020,"licensePlate":"ABC1D23",
                 "passengerCapacity":4,"color":"Prata"}
                """;
        mockMvc.perform(post("/api/drivers/me/vehicles")
                        .with(jwtFor(driverUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(vehicleBody))
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(post("/api/admin/drivers/{id}/approval", driver.getId())
                        .with(jwt().jwt(token -> token.subject("999"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(post("/api/drivers/me/vehicles")
                        .with(jwtFor(driverUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(vehicleBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.defaultVehicle").value(true));

        String replacementVehicleBody = """
                {"brand":"Fiat","model":"Ducato","year":2022,"licensePlate":"DEF4G56",
                 "passengerCapacity":15,"color":"Branca"}
                """;
        mockMvc.perform(post("/api/drivers/me/vehicles")
                        .with(jwtFor(driverUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(replacementVehicleBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.defaultVehicle").value(false));
        long replacementVehicleId = vehicleRepository.findByLicensePlateIgnoreCase("DEF4G56")
                .orElseThrow().getId();

        mockMvc.perform(put("/api/drivers/me/vehicles/{id}/default", replacementVehicleId)
                        .with(jwtFor(driverUser)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultVehicle").value(true));

        mockMvc.perform(put("/api/drivers/me/operational-address")
                        .with(jwtFor(driverUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"street":"Rua da Garagem","number":"50","neighborhood":"Centro",
                                 "city":"Salvador","state":"BA","zipCode":"40000-100"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationalAddress.street").value("Rua da Garagem"));

        mockMvc.perform(post("/api/drivers/me/invites")
                        .with(jwtFor(driverUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"replaceCurrent\":false}"))
                .andExpect(status().isCreated());

        String token = inviteRepository.findAllByDriverId(driver.getId()).getFirst().getToken();
        mockMvc.perform(post("/api/invites/preview")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverName").value("Motorista Controller"));

        User studentUser = saveUser("Aluno Vinculado", "linked-student@test.local", Role.STUDENT);
        studentService.register(studentUser.getId());
        Long institutionId = createInstitution("Instituição do Aluno Vinculado");
        studentService.selectInstitution(studentUser.getId(), institutionId);
        scheduleService.setForDay(studentUser.getId(), java.time.DayOfWeek.TUESDAY,
                LocalTime.of(7, 30), null);

        mockMvc.perform(post("/api/students/me/links")
                        .with(jwtFor(studentUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.driverId").value(driver.getId()));

        mockMvc.perform(get("/api/drivers/me/students").with(jwtFor(driverUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aluno Vinculado"))
                .andExpect(jsonPath("$[0].profileComplete").value(true))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].dateOfBirth").doesNotExist());
    }

    private User saveUser(String name, String email, Role role) {
        return userRepository.save(User.builder()
                .name(name)
                .email(email)
                .password("encoded-for-controller-test")
                .phone("71999999999")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .emailVerifiedAt(LocalDateTime.now())
                .role(role)
                .address(Address.builder()
                        .street("Rua Teste").number("1").neighborhood("Centro")
                        .city("Salvador").state("BA").zipCode("40000-000").build())
                .build());
    }

    private Long createInstitution(String name) {
        return institutionService.create(new InstitutionRequest(name, InstitutionType.UNIVERSITY,
                new AddressRequest("Rua da Instituição", "2", null, "Centro",
                        "Salvador", "BA", "40000-001"))).id();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(User user) {
        return jwt().jwt(token -> token.subject(user.getId().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }
}
