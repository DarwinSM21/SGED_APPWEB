package org.uteq.backend.auth.security;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Esta clase se utiliza para generar contraseñas cifradas utilizando BCryptPasswordEncoder. 
// Esta clase es temporal y se puede eliminar después de generar las contraseñas necesarias para los usuarios.
public class PasswordGenerator {

    public static void main(String[] args) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println(
            encoder.encode("admin123")
        );
    }
}
