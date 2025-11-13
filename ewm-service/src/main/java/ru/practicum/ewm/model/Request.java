package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.enums.RequestStatus;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "requests")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "created")
    LocalDateTime created; // Дата и время создания заявки

    @ManyToOne
    @JoinColumn(name = "event_id")
    Event event; // Идентификатор события

    @ManyToOne
    @JoinColumn(name = "requester_id")
    User requester; // Идентификатор пользователя, отправившего заявку

    @Enumerated(EnumType.STRING)
    RequestStatus status; // Статус заявки

    public Request(LocalDateTime created, Event event, User requester, RequestStatus status) {
        this.created = created;
        this.event = event;
        this.requester = requester;
        this.status = status;
    }


}
