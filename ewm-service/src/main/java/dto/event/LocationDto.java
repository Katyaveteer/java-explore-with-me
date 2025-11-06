package dto.event;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Float lat;
    private Float lon;
}