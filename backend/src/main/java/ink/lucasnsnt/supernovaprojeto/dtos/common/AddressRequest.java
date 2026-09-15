package ink.lucasnsnt.supernovaprojeto.dtos.common;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AddressRequest(
        @NotBlank String street,
        @NotBlank String number,
        String complement,
        @NotBlank String neighborhood,
        @NotBlank String city,
        @NotBlank @Size(min = 2, max = 2) String state,
        @NotBlank @Pattern(regexp = "\\d{5}-?\\d{3}") String zipCode,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude) {

    public AddressRequest(
            String street,
            String number,
            String complement,
            String neighborhood,
            String city,
            String state,
            String zipCode) {
        this(street, number, complement, neighborhood, city, state, zipCode, null, null);
    }

    @AssertTrue(message = "latitude e longitude devem ser informadas juntas")
    public boolean isCoordinatesComplete() {
        return (latitude == null) == (longitude == null);
    }
}
