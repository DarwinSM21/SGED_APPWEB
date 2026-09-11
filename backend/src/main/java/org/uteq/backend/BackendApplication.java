package org.uteq.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Punto de entrada del backend de SGED. Habilita el escaneo automático de
 * componentes de Spring Boot y la caché declarativa ({@code @Cacheable} en
 * los listados, ver RNF-02).
 */
@SpringBootApplication
@EnableCaching
public class BackendApplication {

	/**
	 * @param args argumentos de línea de comandos, delegados a Spring Boot sin modificación
	 */
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
