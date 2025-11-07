package mapper;

import dto.compilation.CompilationDto;
import dto.compilation.NewCompilationDto;
import dto.compilation.UpdateCompilationRequest;
import model.Compilation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CompilationMapper {

    private final EventMapper eventMapper;

    @Autowired
    public CompilationMapper(EventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    public CompilationDto toCompilationDto(Compilation compilation) {
        if (compilation == null) {
            return null;
        }

        CompilationDto compilationDto = new CompilationDto();
        compilationDto.setId(compilation.getId());

        if (compilation.getEvents() != null) {
            compilationDto.setEvents(
                    compilation.getEvents().stream()
                            .map(eventMapper::toEventShortDto)
                            .collect(Collectors.toList())
            );
        }

        compilationDto.setPinned(compilation.getPinned());
        compilationDto.setTitle(compilation.getTitle());

        return compilationDto;
    }

    public Compilation toCompilation(NewCompilationDto newCompilationDto) {
        if (newCompilationDto == null) {
            return null;
        }

        Compilation compilation = new Compilation();
        compilation.setPinned(newCompilationDto.getPinned() != null ? newCompilationDto.getPinned() : false);
        compilation.setTitle(newCompilationDto.getTitle());

        return compilation;
    }

    public Compilation toCompilation(UpdateCompilationRequest updateRequest) {
        if (updateRequest == null) {
            return null;
        }

        Compilation compilation = new Compilation();
        compilation.setPinned(updateRequest.getPinned());
        compilation.setTitle(updateRequest.getTitle());

        return compilation;
    }
}
