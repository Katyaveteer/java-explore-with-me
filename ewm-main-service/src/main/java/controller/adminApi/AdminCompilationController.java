package controller.adminApi;

import dto.compilation.CompilationDto;
import dto.compilation.NewCompilationDto;
import dto.compilation.UpdateCompilationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import service.adminService.AdminCompilationService;

@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
public class AdminCompilationController {

    private final AdminCompilationService service;

    @PostMapping
    public CompilationDto saveCompilation(@Valid @RequestBody NewCompilationDto dto) {
        return service.saveCompilation(dto);
    }

    @PatchMapping("/{compId}")
    public CompilationDto updateCompilation(@PathVariable Long compId,
                                            @Valid @RequestBody UpdateCompilationRequest dto) {
        return service.updateCompilation(compId, dto);
    }

    @DeleteMapping("/{compId}")
    public void deleteCompilation(@PathVariable Long compId) {
        service.deleteCompilation(compId);
    }
}
