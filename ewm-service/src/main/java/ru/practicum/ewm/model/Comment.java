package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "comments")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "created")
    LocalDateTime created; // Дата и время создания комментария

    @ManyToOne
    @JoinColumn(name = "event_id")
    Event event; // Cобытие, к которому написан комментарий

    @ManyToOne
    @JoinColumn(name = "commentator_id")
    User commentator; // Пользователь, который написал комментарий

    @Column(name = "comment_text")
    String commentText;
}