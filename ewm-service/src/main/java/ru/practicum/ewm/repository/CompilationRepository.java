package ru.practicum.ewm.repository;


import org.springframework.data.domain.Page;
import ru.practicum.ewm.model.Compilation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    Page<Compilation> findAllByPinnedOrderByIdDesc(Boolean pinned, Pageable pageable);

    Page<Compilation> findAll(Pageable pageable);

}
