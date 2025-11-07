package ru.practicum.ewm.controller.publicApi;


import lombok.extern.slf4j.Slf4j;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.service.compilation.CompilationService;

import java.util.List;

@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PublicCompilationController {
    private final CompilationService compilationService;


    @GetMapping
    public List<CompilationDto> getCompilationList(@RequestParam(required = false) Boolean pinned,
                                                   @RequestParam(required = false, defaultValue = "0") @PositiveOrZero Integer from,
                                                   @RequestParam(required = false, defaultValue = "10") @Positive Integer size) {
        log.info("PubCompilationController / getCompilationList: получение подборок событий " + pinned + from + size);
        return compilationService.getCompilationList(pinned, from, size);
    }


    @GetMapping("/{compId}")
    public CompilationDto getCompilation(@PathVariable Long compId) {
        log.info("PubCompilationController / getCompilation: получение подборки событие по его id " + compId);
        return compilationService.getCompilation(compId);
    }
}