package model;

import enums.EventState;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@Table(name = "events")
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String annotation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Embedded
    private Location location;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    @ManyToOne
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Builder.Default
    @Column(nullable = false)
    private Boolean paid = false;


    @Builder.Default
    @Column(nullable = false)
    private Integer participantLimit = 0;


    @Builder.Default
    @Column(nullable = false)
    private Boolean requestModeration = true;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EventState state = EventState.PENDING;

    @Builder.Default
    private LocalDateTime createdOn = LocalDateTime.now();
    private LocalDateTime publishedOn;
}
