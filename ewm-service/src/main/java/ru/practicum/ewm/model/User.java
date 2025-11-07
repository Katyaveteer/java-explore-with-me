package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @OneToMany(mappedBy = "initiator")
    private Set<Event> events;

    @OneToMany(mappedBy = "requester")
    private Set<ParticipationRequest> requests;
}