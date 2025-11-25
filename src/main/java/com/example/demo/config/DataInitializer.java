package com.example.demo.config;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final ScrapperRepository scrapperRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductoRepository productoRepository;
    private final MembresiaRepository membresiaRepository;

    @Override
    public void run(String... args) {
        initRoles();
        initUsers();
        initScrappers();
        initProductos();
    }

    private void initRoles() {
        // Rol 1: admin
        rolRepository.findByNombre("admin").orElseGet(() ->
                rolRepository.save(
                        Rol.builder()
                                .nombre("admin")
                                .descripcion("Todos los accesos")
                                .build()
                )
        );

        // Rol 2: user
        rolRepository.findByNombre("user").orElseGet(() ->
                rolRepository.save(
                        Rol.builder()
                                .nombre("user")
                                .descripcion("usuario")
                                .build()
                )
        );

        // Rol 3: Premiun
        rolRepository.findByNombre("Premiun").orElseGet(() ->
                rolRepository.save(
                        Rol.builder()
                                .nombre("Premiun")
                                .descripcion("usuario premiun")
                                .build()
                )
        );
    }

    private void initUsers() {
        // Obtenemos los roles que necesitamos
        Rol adminRole = rolRepository.findByNombre("admin")
                .orElseThrow(() -> new RuntimeException("Rol admin no encontrado"));

        Rol userRole = rolRepository.findByNombre("user")
                .orElseThrow(() -> new RuntimeException("Rol user no encontrado"));

        // Usuario 1: Andres
        if (!usuarioRepository.existsByUsername("andres.montes")) {
            Usuario u1 = Usuario.builder()
                    .name("Andres Lino")
                    .lastName("Montes Mamani")
                    .number("959746720")
                    .username("andres.montes")
                    .email("linoplas0@gmail.com")
                    .passwordHash(passwordEncoder.encode("12345")) // se encripta aquí
                    .rol(adminRole)
                    .build();

            usuarioRepository.save(u1);
        }

        // Usuario 2: Deysy
        if (!usuarioRepository.existsByUsername("deysy.muñuico")) {
            Usuario u2 = Usuario.builder()
                    .name("Deysy")
                    .lastName("Muñuico Muñuico")
                    .number("998327756")
                    .username("deysy.muñuico")
                    .email("deysy@gmail.com")
                    .passwordHash(passwordEncoder.encode("12345")) // se encripta aquí
                    .rol(userRole)
                    .build();

            usuarioRepository.save(u2);
        }
        // Usuario 3: Deysy
        if (!usuarioRepository.existsByUsername("cristian.cabana")) {
            Usuario u2 = Usuario.builder()
                    .name("Cristian")
                    .lastName("Cabana Sulca")
                    .number("998327756")
                    .username("cristian.cabana")
                    .email("crisho@gmail.com")
                    .passwordHash(passwordEncoder.encode("12345")) // se encripta aquí
                    .rol(userRole)
                    .build();

            usuarioRepository.save(u2);
        }
        // Usuario 5: Deysy
        if (!usuarioRepository.existsByUsername("alessandro.mamani")) {
            Usuario u2 = Usuario.builder()
                    .name("Alessandro Pastor")
                    .lastName("Mamani Mamani")
                    .number("998327756")
                    .username("alessandro.mamani")
                    .email("alex@gmail.com")
                    .passwordHash(passwordEncoder.encode("12345")) // se encripta aquí
                    .rol(userRole)
                    .build();

            usuarioRepository.save(u2);
        }
    }

    private void initScrappers() {
        // Scrapper 1: RPP
        if (!scrapperRepository.existsByUrl("https://rpp.pe/")) {
            Scrapper s1 = Scrapper.builder()
                    .url("https://rpp.pe/")
                    .nombrePagina("RPP Noticias")
                    .logo("https://peru.mom-gmr.org/uploads/tx_lfrogmom/media/2020-205_import.png")
                    .build();

            scrapperRepository.save(s1);
        }

        // Scrapper 2: Diario Sin Fronteras
        if (!scrapperRepository.existsByUrl("https://diariosinfronteras.com.pe/")) {
            Scrapper s2 = Scrapper.builder()
                    .url("https://diariosinfronteras.com.pe/")
                    .nombrePagina("Diario Sin Fronteras")
                    .logo("https://diariosinfronteras.com.pe/wp-content/uploads/2020/06/diariosinfronteras.jpg")
                    .build();

            scrapperRepository.save(s2);
        }
    }

    private void initProductos() {

        if (!productoRepository.existsByNombre("Premium Mensual")) {
            Producto p1 = Producto.builder()
                    .nombre("Premium Mensual")
                    .descripcion("Acceso completo por 30 días")
                    .precio(29.99)
                    .tipo("SUSCRIPCION")
                    .estado("ACTIVO")
                    .build();

            productoRepository.save(p1);
        }

        if (!productoRepository.existsByNombre("Premium Anual")) {
            Producto p2 = Producto.builder()
                    .nombre("Premium Anual")
                    .descripcion("Acceso completo por 12 meses")
                    .precio(249.99)
                    .tipo("SUSCRIPCION")
                    .estado("ACTIVO")
                    .build();

            productoRepository.save(p2);
        }

        if (!productoRepository.existsByNombre("Clásico Mensual")) {
            Producto p3 = Producto.builder()
                    .nombre("Clásico Mensual")
                    .descripcion("Acceso estándar por 30 días")
                    .precio(14.99)
                    .tipo("SUSCRIPCION")
                    .estado("ACTIVO")
                    .build();

            productoRepository.save(p3);
        }
    }
}
