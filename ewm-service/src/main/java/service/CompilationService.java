package service;


import dto.compilation.CompilationDto;
import dto.compilation.NewCompilationDto;
import dto.compilation.UpdateCompilationRequest;
import exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import mapper.CompilationMapper;
import model.Compilation;
import model.Event;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.CompilationRepository;
import repository.EventRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations;

        if (pinned != null) {
            compilations = compilationRepository.findByPinned(pinned, pageable);
        } else {
            compilations = compilationRepository.findAll(pageable).getContent();
        }
        return compilations.stream()
                .map(compilationMapper::toCompilationDto)
                .collect(Collectors.toList());
    }

    public CompilationDto getCompilation(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        return compilationMapper.toCompilationDto(compilation);
    }


    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        Set<Event> events = new HashSet<>();
        if (newCompilationDto.getEvents() != null) {
            events = new HashSet<>(eventRepository.findByIdIn(newCompilationDto.getEvents()));
        }

        Compilation compilation = compilationMapper.toCompilation(newCompilationDto);
        compilation.setEvents(events);

        Compilation savedCompilation = compilationRepository.save(compilation);
        return compilationMapper.toCompilationDto(savedCompilation);
    }

    @Transactional
    public void deleteCompilation(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }
        compilationRepository.deleteById(compId);
    }

    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        if (updateRequest.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findByIdIn(updateRequest.getEvents()));
            compilation.setEvents(events);
        }
        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }
        if (updateRequest.getTitle() != null) {
            compilation.setTitle(updateRequest.getTitle());
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        return compilationMapper.toCompilationDto(updatedCompilation);
    }
}