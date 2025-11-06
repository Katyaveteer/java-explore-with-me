package service.adminService;


import dto.compilation.CompilationDto;
import dto.compilation.NewCompilationDto;
import dto.compilation.UpdateCompilationRequest;
import exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import model.Compilation;
import model.Event;
import org.springframework.stereotype.Service;
import repository.CompilationRepository;
import repository.EventRepository;
import service.publicService.PublicEventService;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final PublicEventService publicEventService;

    public CompilationDto saveCompilation(NewCompilationDto dto) {
        Set<Event> events = new HashSet<>();
        if (dto.getEvents() != null) {
            events = new HashSet<>(eventRepository.findAllById(dto.getEvents()));
        }
        Compilation compilation = Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned())
                .events(events)
                .build();
        compilation = compilationRepository.save(compilation);
        return toDto(compilation);
    }

    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest dto) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        if (dto.getTitle() != null) compilation.setTitle(dto.getTitle());
        if (dto.getPinned() != null) compilation.setPinned(dto.getPinned());
        if (dto.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(dto.getEvents()));
            compilation.setEvents(events);
        }

        compilation = compilationRepository.save(compilation);
        return toDto(compilation);
    }

    public void deleteCompilation(Long compId) {
        compilationRepository.deleteById(compId);
    }

    private CompilationDto toDto(Compilation c) {
        return CompilationDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .pinned(c.getPinned())
                .events(c.getEvents().stream().map(publicEventService::toShortDto).collect(Collectors.toList()))
                .build();
    }
}
