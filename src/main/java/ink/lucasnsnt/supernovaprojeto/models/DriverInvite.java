package ink.lucasnsnt.supernovaprojeto.models;

import ink.lucasnsnt.supernovaprojeto.models.enums.InviteStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_invite")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DriverInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    Driver driver;

    String token;           // usado na URL do convite
    LocalDateTime expiresAt;
    InviteStatus status;    // PENDING, USED, EXPIRED, REVOKED
    LocalDateTime createdAt;
}