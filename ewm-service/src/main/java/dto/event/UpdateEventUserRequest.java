package dto.event;


import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventUserRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Size(min = 20, max = 2000)
    private String annotation;

    private Long category;

    @Size(min = 20, max = 7000)
    private String description;

    private String eventDate;
    private LocationDto location;
    private Boolean paid;
    private Integer participantLimit;
    private Boolean requestModeration;

    private String stateAction; // "SEND_TO_REVIEW" или "CANCEL_REVIEW"

    @Size(min = 3, max = 120)
    private String title;
}
