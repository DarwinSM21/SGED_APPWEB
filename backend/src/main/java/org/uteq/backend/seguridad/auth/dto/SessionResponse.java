package org.uteq.backend.seguridad.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionResponse {
    private String username;
    private String name;
    private String role;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long personId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long userId;
}
