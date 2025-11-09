package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.enums.EventState;
import ru.practicum.ewm.model.Event;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    boolean existsByCategoryId(Long categoryId);

    Set<Event> findAllByIdIn(Set<Long> eventIdList);

    // для EventService
    Page<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    Optional<Event> findByIdAndState(Long eventId, EventState eventStatus);

    @Query(value = "SELECT * FROM events e " +
            "WHERE (:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) " +
            "OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND (:categories IS NULL OR e.category_id IN :categories) " + // ← здесь просто IN :categories
            "AND e.state = 'PUBLISHED' " +
            "AND (:paid IS NULL OR e.paid = :paid) " + // ← здесь просто = :paid
            "AND e.event_date >= :rangeStart " +
            "AND (:rangeEnd IS NULL OR e.event_date < :rangeEnd) ",
            countQuery = "SELECT COUNT(*) FROM events e " +
                    "WHERE (:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) " +
                    "OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
                    "AND (:categories IS NULL OR e.category_id IN :categories) " +
                    "AND e.state = 'PUBLISHED' " +
                    "AND (:paid IS NULL OR e.paid = :paid) " +
                    "AND e.event_date >= :rangeStart " +
                    "AND (:rangeEnd IS NULL OR e.event_date < :rangeEnd) ",
            nativeQuery = true)
    Page<Event> searchPublishedEvents(@Param("text") String text,
                                      @Param("categories") List<Long> categories,
                                      @Param("paid") Boolean paid,
                                      @Param("rangeStart") LocalDateTime rangeStart,
                                      @Param("rangeEnd") LocalDateTime rangeEnd,
                                      Pageable pageable);

    @Query(value = "SELECT * FROM events e " +
            "WHERE (:userIds IS NULL OR e.initiator_id IN :userIds) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categories IS NULL OR e.category_id IN :categories) " +
            "AND (:rangeStart IS NULL OR e.event_date >= :rangeStart) " +
            "AND (:rangeEnd IS NULL OR e.event_date < :rangeEnd) ",
            countQuery = "SELECT COUNT(*) FROM events e " +
                    "WHERE (:userIds IS NULL OR e.initiator_id IN :userIds) " +
                    "AND (:states IS NULL OR e.state IN :states) " +
                    "AND (:categories IS NULL OR e.category_id IN :categories) " +
                    "AND (:rangeStart IS NULL OR e.event_date >= :rangeStart) " +
                    "AND (:rangeEnd IS NULL OR e.event_date < :rangeEnd) ",
            nativeQuery = true)
    Page<Event> findEvents(@Param("userIds") List<Long> userIds,
                           @Param("states") List<String> states,
                           @Param("categories") List<Long> categories,
                           @Param("rangeStart") LocalDateTime rangeStart,
                           @Param("rangeEnd") LocalDateTime rangeEnd,
                           Pageable pageable);
}
