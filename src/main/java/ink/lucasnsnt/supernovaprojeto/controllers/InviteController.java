package ink.lucasnsnt.supernovaprojeto.controllers;

import ink.lucasnsnt.supernovaprojeto.dtos.invite.InvitePreviewResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.invite.InviteTokenRequest;
import ink.lucasnsnt.supernovaprojeto.services.DriverInviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteController {

    private final DriverInviteService inviteService;

    @PostMapping("/preview")
    public InvitePreviewResponse preview(@Valid @RequestBody InviteTokenRequest request) {
        return inviteService.preview(request.token());
    }
}
