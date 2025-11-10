package ru.practicum.ewm.dto.event;

import lombok.Data;

import java.util.List;

@Data
public class PublicEventFilter {
    private String text;
    private List<Long> categories;
    private Boolean paid;
    private String rangeStart;
    private String rangeEnd;
    private Boolean onlyAvailable;
    private String sort;
    private Integer from;
    private Integer size;
    private String userIp;
    private String requestUri;
}

