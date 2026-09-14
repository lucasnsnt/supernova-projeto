package ink.lucasnsnt.supernovaprojeto.dtos.common;

import ink.lucasnsnt.supernovaprojeto.models.Address;

import java.math.BigDecimal;

public record AddressResponse(
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String zipCode,
        BigDecimal latitude,
        BigDecimal longitude) {

    public static AddressResponse from(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(address.getStreet(), address.getNumber(), address.getComplement(),
                address.getNeighborhood(), address.getCity(), address.getState(), address.getZipCode(),
                address.getLatitude(), address.getLongitude());
    }
}
