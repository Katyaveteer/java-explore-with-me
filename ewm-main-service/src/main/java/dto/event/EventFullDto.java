package dto.event;

import dto.category.CategoryDto;
import dto.user.UserShortDto;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventFullDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String annotation;
    private CategoryDto category;
    private Long confirmedRequests;
    private String createdOn;
    private String description;
    private String eventDate;
    private Long id;
    private UserShortDto initiator;
    private LocationDto location;
    private Boolean paid;
    private Integer participantLimit;
    private String publishedOn;
    private Boolean requestModeration;
    private String state; // PENDING, PUBLISHED, CANCELED
    private String title;
    private Long views;
}
