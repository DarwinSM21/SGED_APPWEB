package org.uteq.backend.seguridad.auth.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SesionResponse {
    private String username;
    private String nombre;
    private String rol;
    private String accessToken;  
    private String refreshToken; 
}