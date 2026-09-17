package ink.lucasnsnt.supernovaprojeto.dtos.route;

import ink.lucasnsnt.supernovaprojeto.models.*;
import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record RecurringRouteResponse(Long id, String name, boolean active, Long vehicleId, String vehicleLabel,
                                     List<Schedule> schedules, List<InstitutionStop> institutions) {
    public record Schedule(DayOfWeek dayOfWeek, Direction direction, LocalTime departureTime, LocalTime responseDeadlineTime) { }
    public record InstitutionStop(Long institutionId, String institutionName, Integer stopOrder,
                                  LocalTime outboundArrivalBy, LocalTime returnDepartureAt) { }
    public static RecurringRouteResponse from(RecurringRoute route) {
        return new RecurringRouteResponse(route.getId(), route.getName(), route.isActive(), route.getVehicle().getId(),
                route.getVehicle().getModel() + " · " + route.getVehicle().getLicensePlate(),
                route.getSchedules().stream().map(item -> new Schedule(item.getDayOfWeek(), item.getDirection(), item.getDepartureTime(), item.getResponseDeadlineTime())).toList(),
                route.getInstitutions().stream().map(item -> new InstitutionStop(item.getInstitution().getId(), item.getInstitution().getName(), item.getStopOrder(), item.getOutboundArrivalBy(), item.getReturnDepartureAt())).toList());
    }
}
