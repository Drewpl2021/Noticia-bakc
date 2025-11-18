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
        initMembresias();
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
                    .precio(29.90)
                    .tipo("SUSCRIPCION")
                    .estado("ACTIVO")
                    .build();

            productoRepository.save(p1);
        }

        if (!productoRepository.existsByNombre("Premium Anual")) {
            Producto p2 = Producto.builder()
                    .nombre("Premium Anual")
                    .descripcion("Acceso completo por 12 meses")
                    .precio(249.90)
                    .tipo("SUSCRIPCION")
                    .estado("ACTIVO")
                    .build();

            productoRepository.save(p2);
        }

        if (!productoRepository.existsByNombre("Clásico Mensual")) {
            Producto p3 = Producto.builder()
                    .nombre("Clásico Mensual")
                    .descripcion("Acceso estándar por 30 días")
                    .precio(15.00)
                    .tipo("SUSCRIPCION")
                    .estado("ACTIVO")
                    .build();

            productoRepository.save(p3);
        }

        if (!productoRepository.existsByNombre("Super Usuario")) {
            Producto p4 = Producto.builder()
                    .nombre("Super Usuario")
                    .descripcion("Acceso ilimitado a todas las funciones")
                    .precio(99.90)
                    .tipo("SUSCRIPCION")
                    .estado("ACTIVO")
                    .build();

            productoRepository.save(p4);
        }
    }

    private void initMembresias() {

        // Buscar usuarios
        var optAndres = usuarioRepository.findByUsername("andres.montes");
        var optDeysy  = usuarioRepository.findByUsername("deysy.muñuico");

        // Buscar productos (planes)
        var optPremiumMensual = productoRepository.findByNombre("Premium Mensual");
        var optBasicoMensual  = productoRepository.findByNombre("Clásico Mensual");

        // Si alguno no existe, salimos sin romper la app
        if (optAndres.isEmpty() || optPremiumMensual.isEmpty()) {
            return;
        }

        if (optDeysy.isEmpty() || optBasicoMensual.isEmpty()) {
            // no pasa nada, solo no se crea la membresía de Deysy
        }

        // Membresía ACTIVA para Andrés
        if (!membresiaRepository.existsByUsuarioUsernameAndEstado("andres.montes", "ACTIVA")) {

            LocalDateTime ahora = LocalDateTime.now();

            Membresia m1 = Membresia.builder()
                    .usuario(optAndres.get())
                    .producto(optPremiumMensual.get())       // FK -> productos.id
                    .fechaInicio(ahora.minusDays(2))
                    .fechaFin(ahora.plusDays(28))            // 30 días
                    .estado("ACTIVA")
                    .paymentReference("SEED-ANDRES-001")
                    .build();

            membresiaRepository.save(m1);
        }

        // Membresía VENCIDA para Deysy
        if (optDeysy.isPresent() && optBasicoMensual.isPresent() &&
                !membresiaRepository.existsByUsuarioUsernameAndEstado("deysy.muñuico", "VENCIDA")) {

            LocalDateTime ahora = LocalDateTime.now();

            Membresia m2 = Membresia.builder()
                    .usuario(optDeysy.get())
                    .producto(optBasicoMensual.get())        // FK -> productos.id
                    .fechaInicio(ahora.minusDays(40))
                    .fechaFin(ahora.minusDays(10))           // ya venció
                    .estado("VENCIDA")
                    .paymentReference("SEED-DEYSY-001")
                    .build();

            membresiaRepository.save(m2);
        }
    }


}
