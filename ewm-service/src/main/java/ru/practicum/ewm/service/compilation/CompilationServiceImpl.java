package ru.practicum.ewm.service.compilation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.enums.RequestStatus;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CompilationMapper;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.RequestRepository;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.statsdto.ViewStats;
import ru.practicum.statsdto.ViewStatsRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;


    @Override
    public List<CompilationDto> getCompilationList(Boolean pinned, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);

        Page<Compilation> compilationPage;

        if (pinned != null) {
            compilationPage = compilationRepository.findAllByPinnedOrderByIdDesc(pinned, pageable);
        } else {
            compilationPage = compilationRepository.findAll(pageable);
        }

        List<Compilation> compilationList = compilationPage.getContent();

        List<CompilationDto> compilationDtoList = new ArrayList<>();

        for (Compilation compilation : compilationList) {
            CompilationDto compilationDto = compilationMapper.toCompilationDto(compilation);
            compilationDtoList.add(addConfirmedRequestsAndViews(compilationDto));
        }

        return compilationDtoList;
    }


    @Override
    public CompilationDto getCompilation(Long compilationId) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Compilation does not exist with id" + compilationId));


        CompilationDto compilationDto = compilationMapper.toCompilationDto(compilation);
        return addConfirmedRequestsAndViews(compilationDto);
    }


    @Override
    public CompilationDto addCompilation(NewCompilationDto newCompilationDto) {

        if (newCompilationDto.getEvents() != null && newCompilationDto.getEvents().size() != 0) {

            Set<Long> eventIdList = newCompilationDto.getEvents();


            Set<Event> events = eventRepository.findAllByIdIn(eventIdList);
            Compilation compilation = compilationMapper.toCompilation(newCompilationDto);


            compilation.setEvents(events);
            compilationRepository.save(compilation);


            CompilationDto compilationDto = compilationMapper.toCompilationDto(compilation);
            return addConfirmedRequestsAndViews(compilationDto);
        }


        Compilation compilation = compilationMapper.toCompilation(newCompilationDto);
        if (compilation.getEvents() == null) {
            compilation.setEvents(new HashSet<>());
        }
        compilationRepository.save(compilation);
        return compilationMapper.toCompilationDto(compilation);
    }


    @Override
    public void deleteCompilation(Long compilationId) {
        compilationRepository.findById(compilationId).orElseThrow(() ->
                new NotFoundException("Compilation does not exist with id" + compilationId));
        compilationRepository.deleteById(compilationId);
    }


    @Override
    public CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest updateCompilationRequest) {

        Compilation compilation = compilationRepository.findById(compilationId).orElseThrow(() ->
                new NotFoundException("Compilation does not exist with id" + compilationId));


        if (updateCompilationRequest.getEvents() != null && !updateCompilationRequest.getEvents().isEmpty()) {
            Set<Long> eventIdList = updateCompilationRequest.getEvents();
            Set<Event> events = eventRepository.findAllByIdIn(eventIdList);
            compilation.setEvents(events);
        }

        if (updateCompilationRequest.getPinned() != null) {
            compilation.setPinned(updateCompilationRequest.getPinned());
        }

        if (updateCompilationRequest.getTitle() != null) {
            compilation.setTitle(updateCompilationRequest.getTitle());
        }

        compilationRepository.save(compilation);

        CompilationDto compilationDto = compilationMapper.toCompilationDto(compilation);

        return addConfirmedRequestsAndViews(compilationDto);
    }

    private CompilationDto addConfirmedRequestsAndViews(CompilationDto compilationDto) {
        for (EventShortDto eventDto : compilationDto.getEvents()) {

            eventDto.setConfirmedRequests(
                    requestRepository.countByEventIdAndStatus(eventDto.getId(), RequestStatus.CONFIRMED));


            List<String> uris = new ArrayList<>();


            uris.add("/events/" + eventDto.getId());
            ViewStatsRequest viewStatsRequest = new ViewStatsRequest(
                    LocalDateTime.now().minusYears(100),
                    LocalDateTime.now(),
                    uris,
                    true);
            List<ViewStats> viewStatsList = statsClient.getStats(viewStatsRequest);
            if (viewStatsList.isEmpty()) {
                eventDto.setViews(0L);
            } else {
                eventDto.setViews(viewStatsList.getFirst().getHits());
            }
        }
        return compilationDto;
    }
}

