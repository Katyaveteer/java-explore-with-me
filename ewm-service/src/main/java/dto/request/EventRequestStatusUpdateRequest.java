package dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<Long> requestIds;

    @NotNull
    private String status;
}
