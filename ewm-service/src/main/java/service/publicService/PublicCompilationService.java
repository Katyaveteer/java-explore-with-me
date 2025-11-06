package service.publicService;

import dto.compilation.CompilationDto;
import dto.event.EventShortDto;
import exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import model.Compilation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import repository.CompilationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicCompilationService {

    private final CompilationRepository compilationRepository;
    private final PublicEventService publicEventService;

    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        Page<Compilation> pageResult;
        if (pinned == null) {
            pageResult = compilationRepository.findAll(page);
        } else {
            pageResult = compilationRepository.findByPinned(pinned, page);
        }
        return pageResult.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CompilationDto getCompilation(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        return toDto(compilation);
    }

    private CompilationDto toDto(Compilation c) {
        List<EventShortDto> events = c.getEvents().stream()
                .map(publicEventService::toShortDto)
                .collect(Collectors.toList());
        return CompilationDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .pinned(c.getPinned())
                .events(events)
                .build();
    }
}