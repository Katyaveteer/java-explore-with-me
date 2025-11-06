package dto.compilation;

import dto.event.EventShortDto;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompilationDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<EventShortDto> events;
    private Long id;
    private Boolean pinned;
    private String title;
}