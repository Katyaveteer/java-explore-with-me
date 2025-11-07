package ru.practicum.ewm.repository;


import org.springframework.data.domain.Page;
import ru.practicum.ewm.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Page<User> findAllByIdIn(List<Long> idList, Pageable pageable);

    Boolean existsByName(String name);

    Boolean existsByIdIn(List<Long> userId);
}
