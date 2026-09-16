package ink.lucasnsnt.supernovaprojeto.controllers;

import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.common.GeocodePreviewResponse;
import ink.lucasnsnt.supernovaprojeto.services.GeocodingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/geocoding")
@RequiredArgsConstructor
public class GeocodingController {

    private final GeocodingService geocodingService;

    @PostMapping("/preview")
    public GeocodePreviewResponse preview(@Valid @RequestBody AddressRequest request) {
        return geocodingService.preview(request);
    }
}
