package dto.request;


import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequestDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String created;
    private Long event;
    private Long id;
    private Long requester;
    private String status;
}
