package ru.practicum.ewm.mapper;

import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.model.ParticipationRequest;
import org.springframework.stereotype.Component;


@Component
public class RequestMapper {

    public ParticipationRequestDto toParticipationRequestDto(ParticipationRequest request) {
        if (request == null) {
            return null;
        }

        ParticipationRequestDto requestDto = new ParticipationRequestDto();
        requestDto.setId(request.getId());
        requestDto.setCreated(request.getCreated());
        requestDto.setEvent(request.getEvent() != null ? request.getEvent().getId() : null);
        requestDto.setRequester(request.getRequester() != null ? request.getRequester().getId() : null);
        requestDto.setStatus(request.getStatus());

        return requestDto;
    }

    public ParticipationRequest toParticipationRequest(ParticipationRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setId(requestDto.getId());
        request.setCreated(requestDto.getCreated());
        request.setStatus(requestDto.getStatus());

        return request;
    }
}
